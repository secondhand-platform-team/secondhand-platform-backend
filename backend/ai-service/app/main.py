"""
ReLife AI Service — FastAPI Application
========================================
Microservice xử lý AI cho nền tảng ReLife:
  - AI Chat (Gemini LLM)
  - Recommendation (gợi ý sản phẩm)
  - Phân tích hành vi (future)
  - ML Training (future)

Chạy:
  uvicorn app.main:app --reload --port 8085
"""
import logging
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routes import chat, recommendation
from app.services.core_client import close_http_client

# Load .env
load_dotenv()

# Logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


# ── Lifecycle ───────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("🚀 AI Service đang khởi động...")
    yield
    # Cleanup
    await close_http_client()
    logger.info("🛑 AI Service đã dừng.")


# ── App ─────────────────────────────────────────────────────────

app = FastAPI(
    title="ReLife AI Service",
    description="Microservice xử lý AI Chat, Recommendation, và Machine Learning cho nền tảng ReLife.",
    version="1.0.0",
    lifespan=lifespan,
)

# ── CORS ────────────────────────────────────────────────────────

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",   # web-client (Next.js)
        "http://localhost:5173",   # web-admin (Vite)
        "http://localhost:5174",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Routes ──────────────────────────────────────────────────────

app.include_router(chat.router)
app.include_router(recommendation.router)


# ── Health check ────────────────────────────────────────────────

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
