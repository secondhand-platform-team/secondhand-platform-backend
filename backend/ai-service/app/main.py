"""
ReLife AI Service — FastAPI Application
========================================
"""
import logging
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routes import chat, recommendation
from app.services.core_client import close_http_client

load_dotenv()
settings = get_settings()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("AI Service starting")
    yield
    await close_http_client()
    logger.info("AI Service stopped")


app = FastAPI(
    title="ReLife AI Service",
    description="Microservice xử lý AI Chat, Recommendation và Machine Learning cho nền tảng ReLife.",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin.strip() for origin in settings.cors_allowed_origins.split(",") if origin.strip()],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router)
app.include_router(recommendation.router)


@app.get("/", tags=["Health"])
async def root():
    return {
        "service": "ReLife AI Service",
        "status": "running",
        "version": "1.0.0",
    }


@app.get("/health", tags=["Health"])
async def health():
    return {"status": "healthy"}