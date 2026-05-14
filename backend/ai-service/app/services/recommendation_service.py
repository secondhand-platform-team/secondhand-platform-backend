"""
Recommendation Service — Gợi ý sản phẩm.

Chiến lược đơn giản cho MVP:
- similar: sản phẩm cùng category
- popular: sản phẩm nổi bật
- Có thể mở rộng thêm ML-based recommendation sau
"""
import logging
from typing import Optional

from app.models.schemas import ProductResult, RecommendationResponse
from app.services import core_client

logger = logging.getLogger(__name__)





async def get_recommendations(
    user_id: Optional[str] = None,
    item_id: Optional[str] = None,
    category_id: Optional[str] = None,
    recent_items: Optional[list[str]] = None,
    cart_item_ids: Optional[list[str]] = None,
    ordered_item_ids: Optional[list[str]] = None,
    limit: int = 8,
) -> RecommendationResponse:
    """
    Gợi ý sản phẩm sử dụng AI/ML dựa trên:
    - Thói quen xem của user (recent_items) -> Gọi LLM phân tích và chọn
    - Fallback về similar / category / popular
    """
    strategy = "popular"
    recent_items = recent_items or []
    cart_item_ids = cart_item_ids or []
    ordered_item_ids = ordered_item_ids or []

    # Thu thập tên các sản phẩm từ cart và order để làm giàu data cho AI
    # (Tránh query quá nhiều, ta chỉ lấy tối đa 5 sản phẩm mỗi loại)
    habit_keywords = list(recent_items)
    
    for c_id in cart_item_ids[:5]:
        it = await core_client.get_item_by_id(c_id)
        if it and it.get("title"):
            habit_keywords.append(it["title"] + " (đang nằm trong giỏ hàng)")
            
    for o_id in ordered_item_ids[:5]:
        it = await core_client.get_item_by_id(o_id)
        if it and it.get("title"):
            habit_keywords.append(it["title"] + " (đã từng mua)")

    # Lấy pool sản phẩm để AI chọn (khoảng 30 sản phẩm nổi bật hoặc mới)
    pool_items = await core_client.get_featured_items(limit=30)
    
    # Nếu có thói quen user (recent_items, cart, order) => Dùng LLM để gợi ý cá nhân hóa
    if habit_keywords and pool_items:
        strategy = "ai_personalized"
        try:
            import json
            from app.services.chat_service import LLM_CLIENTS
            
            # Chuẩn bị dữ liệu cho LLM
            pool_data = []
            for item in pool_items:
                pool_data.append({
                    "id": item.get("itemId"),
                    "name": item.get("title"),
                    "price": item.get("price")
                })
                
            prompt = f"""Bạn là một AI chuyên gia gợi ý sản phẩm mua sắm.
Dưới đây là DỮ LIỆU LỊCH SỬ và THÓI QUEN CỦA NGƯỜI DÙNG NÀY:
{json.dumps(habit_keywords, ensure_ascii=False)}

Dưới đây là danh sách các sản phẩm đang có sẵn để gợi ý:
{json.dumps(pool_data, ensure_ascii=False)}

NHIỆM VỤ CỦA BẠN: 
Dựa vào những gì người dùng đã xem, đã thêm vào giỏ hàng hoặc đã từng mua, hãy chọn ra CHÍNH XÁC {limit} sản phẩm có độ liên quan cao nhất, tương đồng về sở thích nhất từ danh sách đang có sẵn.
Trả về một JSON object chứa mảng các ID sản phẩm được chọn, format:
{{"recommended_ids": ["id1", "id2", ...]}}
"""
            raw_text = None
            for c in LLM_CLIENTS:
                try:
                    response = await c["client"].chat.completions.create(
                        model=c["model"],
                        messages=[{"role": "system", "content": prompt}],
                        response_format={ "type": "json_object" }
                    )
                    raw_text = response.choices[0].message.content
                    break
                except Exception:
                    continue
                    
            if raw_text:
                parsed = json.loads(raw_text)
                recommended_ids = parsed.get("recommended_ids", [])
                
                # Filter pool items based on recommended ids
                ai_items = [i for i in pool_items if i.get("itemId") in recommended_ids]
                
                # Nếu AI chọn đủ
                if len(ai_items) > 0:
                    # Bổ sung thêm nếu AI chọn thiếu
                    if len(ai_items) < limit:
                        for p in pool_items:
                            if p not in ai_items and len(ai_items) < limit:
                                ai_items.append(p)
                                
                    return RecommendationResponse(
                        items=ai_items[:limit],
                        strategy=strategy,
                    )
        except Exception as e:
            logger.warning(f"Lỗi khi chạy AI recommendation: {e}")

    # Fallback: Nếu có item_id → lấy category của item đó rồi tìm tương tự
    if item_id:
        item = await core_client.get_item_by_id(item_id)
        if item and item.get("categoryId"):
            category_id = item["categoryId"]
            strategy = "similar"

    # Tìm theo category
    if category_id:
        items = await core_client.search_items(
            category_id=category_id,
            size=limit,
        )
        if items:
            # Loại bỏ chính item_id nếu có
            if item_id:
                items = [i for i in items if i.get("itemId") != item_id]
            return RecommendationResponse(
                items=items[:limit],
                strategy=strategy,
            )

    # Fallback: sản phẩm nổi bật
    return RecommendationResponse(
        items=pool_items[:limit],
        strategy="popular",
    )
