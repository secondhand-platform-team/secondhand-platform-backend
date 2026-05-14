"""
Route: AI Chat
POST /ai/chat  →  Nhận tin nhắn, trả phản hồi AI + sản phẩm gợi ý
"""
from fastapi import APIRouter
from app.models.schemas import ChatRequest, ChatResponse
from app.services import chat_service

router = APIRouter(prefix="/ai", tags=["AI Chat"])


@router.post("/chat", response_model=ChatResponse)
async def ai_chat(request: ChatRequest):
    """
    Endpoint chính cho AI Chatbot.

    Flow:
    - User gửi tin nhắn
    - LLM phân tích intent
    - Nếu cần tìm sản phẩm → gọi core-service
    - Trả về phản hồi + danh sách sản phẩm + gợi ý
    """
    response = await chat_service.chat(
        message=request.message,
        conversation_id=request.conversation_id,
    )
    return response
