"""
Pydantic schemas cho AI Service API.
"""
from pydantic import BaseModel, Field
from typing import Optional


# ── Chat ────────────────────────────────────────────────────────

class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000, description="Tin nhắn từ người dùng")
    conversation_id: Optional[str] = Field(None, description="ID cuộc trò chuyện (để giữ ngữ cảnh)")


class ProductResult(BaseModel):
    item_id: str = ""
    name: str = ""
    price: str = ""
    location: str = ""
    image_url: str = ""


class ChatResponse(BaseModel):
    reply: str = Field(..., description="Phản hồi từ AI")
    products: list[ProductResult] = Field(default_factory=list, description="Danh sách sản phẩm gợi ý")
    suggestions: list[str] = Field(default_factory=list, description="Gợi ý câu hỏi tiếp theo")
    intent: str = Field(default="general", description="Intent được phát hiện")


# ── Recommendation ──────────────────────────────────────────────

class RecommendationRequest(BaseModel):
    user_id: Optional[str] = None
    item_id: Optional[str] = None
    category_id: Optional[str] = None
    recent_items: Optional[list[str]] = Field(default_factory=list, description="Lịch sử xem gần đây của user")
    cart_item_ids: Optional[list[str]] = Field(default_factory=list, description="Các item trong giỏ hàng")
    ordered_item_ids: Optional[list[str]] = Field(default_factory=list, description="Các item đã mua")
    limit: int = Field(default=8, ge=1, le=20)


from typing import Optional, Any

class RecommendationResponse(BaseModel):
    items: list[Any] = Field(default_factory=list)
    strategy: str = Field(default="popular", description="Chiến lược gợi ý đã sử dụng")
