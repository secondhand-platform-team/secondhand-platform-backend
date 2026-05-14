"""
AI Service Configuration
Đọc biến môi trường từ .env file.
"""
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # LLM Keys
    gemini_api_key: str = ""
    openai_api_key: str = ""
    groq_api_key: str = ""
    gemini_model: str = "gemini-2.0-flash"

    # Microservice URLs (qua Kong gateway)
    core_service_url: str = "http://localhost:8000/core"
    auth_service_url: str = "http://localhost:8000/auth"
    order_service_url: str = "http://localhost:8000/order"

    # Server
    ai_service_port: int = 8085

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
