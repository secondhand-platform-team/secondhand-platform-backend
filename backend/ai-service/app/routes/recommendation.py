"""
Route: Recommendation
POST /ai/recommend  →  Gợi ý sản phẩm cho user
"""
from fastapi import APIRouter
from app.models.schemas import RecommendationRequest, RecommendationResponse
from app.services import recommendation_service

router = APIRouter(prefix="/ai", tags=["Recommendation"])


@router.post("/recommend", response_model=RecommendationResponse)
async def recommend(request: RecommendationRequest):
    """
    Gợi ý sản phẩm dựa trên:
    - item_id: sản phẩm tương tự
    - category_id: sản phẩm cùng danh mục
    - user_id: sản phẩm theo sở thích (future)
    - fallback: sản phẩm nổi bật
    """
    response = await recommendation_service.get_recommendations(
        user_id=request.user_id,
        item_id=request.item_id,
        category_id=request.category_id,
        limit=request.limit,
    )
    return response
