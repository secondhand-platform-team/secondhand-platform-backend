"""
AI Service Configuration
Đọc biến môi trường từ .env file.
"""
from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    gemini_api_key: str = ""
    openai_api_key: str = ""
    groq_api_key: str = ""
    gemini_model: str = "gemini-2.0-flash"

    core_service_url: str = "http://kong:8000/core"
    auth_service_url: str = "http://kong:8000/auth"
    order_service_url: str = "http://kong:8000/order"

    ai_service_port: int = 8085
    cors_allowed_origins: str = "http://localhost:3000,http://localhost:5173,http://localhost:5174"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()