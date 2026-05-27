"""
AI Chat Service — Sử dụng OpenAI LLM.
"""
import json
import logging
import re
from typing import Optional

# pyrefly: ignore [missing-import]
from openai import AsyncOpenAI

from app.config import get_settings
from app.models.schemas import ChatResponse, ProductResult
from app.services import core_client

logger = logging.getLogger(__name__)
settings = get_settings()

# ── Cấu hình OpenAI / Fallback ──────────────────────────────────
# Danh sách các cấu hình API (fallback từ trên xuống dưới)
LLM_CLIENTS = []
if settings.openai_api_key:
    LLM_CLIENTS.append({
        "client": AsyncOpenAI(api_key=settings.openai_api_key),
        "model": "gpt-4o-mini"
    })
if settings.gemini_api_key:
    LLM_CLIENTS.append({
        "client": AsyncOpenAI(
            api_key=settings.gemini_api_key, 
            base_url="https://generativelanguage.googleapis.com/v1beta/openai/"
        ),
        "model": settings.gemini_model or "gemini-2.0-flash"
    })
if settings.groq_api_key:
    LLM_CLIENTS.append({
        "client": AsyncOpenAI(
            api_key=settings.groq_api_key, 
            base_url="https://api.groq.com/openai/v1"
        ),
        "model": "llama-3.3-70b-versatile" # Groq fallback
    })

# ── System prompt ───────────────────────────────────────────────
SYSTEM_PROMPT = """Bạn là ReBot — trợ lý AI của nền tảng ReLife, chuyên mua bán đồ cũ (secondhand).

NHIỆM VỤ:
- Giúp người dùng tìm sản phẩm secondhand
- Hỗ trợ đăng tin bán đồ cũ
- Trả lời câu hỏi về nền tảng ReLife
- Gợi ý sản phẩm phù hợp

LUẬT:
- Trả lời bằng tiếng Việt, thân thiện và ngắn gọn
- Không bịa thông tin sản phẩm — chỉ dùng dữ liệu thật từ hệ thống
- Luôn trả về một object JSON hợp lệ theo định dạng sau.

KHI USER MUỐN TÌM SẢN PHẨM:
{
  "intent": "search_product",
  "query": "từ khóa tìm kiếm (phải thật NGẮN GỌN và TỔNG QUÁT, ví dụ: 'bếp' thay vì 'bếp lò', 'điện thoại' thay vì 'điện thoại cũ')",
  "maxPrice": null hoặc số (VND),
  "minPrice": null hoặc số (VND),
  "condition": null hoặc "NEW" | "LIKE_NEW" | "USED",
  "category_hint": "gợi ý danh mục",
  "suggestions": ["Gợi ý 1", "Gợi ý 2"]
}

KHI USER HỎI VỀ NỀN TẢNG HOẶC CẦN HƯỚNG DẪN ĐĂNG TIN/AN TOÀN:
{
  "intent": "general",
  "reply": "Câu trả lời của bạn",
  "suggestions": ["Gợi ý 1", "Gợi ý 2"]
}
"""

# ── Conversation memory (in-memory, đơn giản cho MVP) ──────────

_conversations: dict[str, list[dict]] = {}
MAX_HISTORY = 10

def _get_history(conversation_id: Optional[str]) -> list[dict]:
    if not conversation_id:
        return []
    return _conversations.get(conversation_id, [])

def _save_turn(conversation_id: Optional[str], role: str, text: str):
    if not conversation_id:
        return
    if conversation_id not in _conversations:
        _conversations[conversation_id] = [{"role": "system", "content": SYSTEM_PROMPT}]
    _conversations[conversation_id].append({"role": role, "content": text})
    if len(_conversations[conversation_id]) > MAX_HISTORY * 2 + 1:
        _conversations[conversation_id] = [_conversations[conversation_id][0]] + _conversations[conversation_id][-(MAX_HISTORY * 2):]

# ── Format sản phẩm ────────────────────────────────────────────

def _format_products(items: list[dict]) -> list[ProductResult]:
    results = []
    for item in items[:5]:
        price_val = item.get("price")
        if price_val and price_val > 0:
            price_str = f"{int(price_val):,}đ".replace(",", ".")
        else:
            price_str = "Miễn phí"

        location_parts = []
        loc = item.get("location", {})
        if loc:
            if loc.get("district"):
                location_parts.append(loc["district"])
            if loc.get("city"):
                location_parts.append(loc["city"])

        condition_map = {
            "NEW": "Mới 100%",
            "LIKE_NEW": "Như mới",
            "USED": "Đã sử dụng",
        }
        cond = condition_map.get(item.get("condition", ""), "")

        loc_str = " · ".join(filter(None, [
            "📍 " + ", ".join(location_parts) if location_parts else "Toàn quốc",
            cond
        ]))

        images = item.get("itemImageList", item.get("images", []))
        image_url = images[0].get("imageUrl", "") if images else ""

        results.append(ProductResult(
            item_id=item.get("itemId", ""),
            name=item.get("title", "Sản phẩm"),
            price=price_str,
            location=loc_str,
            image_url=image_url,
        ))
    return results


# ── Main chat function ──────────────────────────────────────────

async def chat(message: str, conversation_id: Optional[str] = None) -> ChatResponse:
    try:
        if conversation_id and conversation_id not in _conversations:
            _conversations[conversation_id] = [{"role": "system", "content": SYSTEM_PROMPT}]
        
        messages = _get_history(conversation_id) if conversation_id else [{"role": "system", "content": SYSTEM_PROMPT}]
        messages.append({"role": "user", "content": message})

        raw_text = None
        last_error = None
        for c in LLM_CLIENTS:
            try:
                response = await c["client"].chat.completions.create(
                    model=c["model"],
                    messages=messages,
                    response_format={ "type": "json_object" }
                )
                raw_text = response.choices[0].message.content
                break  # Thành công, thoát vòng lặp
            except Exception as e:
                logger.warning(f"Lỗi khi gọi model {c['model']}: {e}")
                last_error = e

        if not raw_text:
            raise Exception(f"Tất cả các API key đều không hoạt động. Lỗi cuối: {last_error}")

        _save_turn(conversation_id, "user", message)
        _save_turn(conversation_id, "assistant", raw_text)

        try:
            parsed = json.loads(raw_text)
        except Exception:
            parsed = {}

        if not parsed:
            return ChatResponse(
                reply="Mình không rõ ý bạn lắm, bạn có thể nói rõ hơn không? 🤔",
                products=[],
                suggestions=["Tìm sản phẩm", "Đăng tin mới", "Hỗ trợ"],
                intent="general",
            )

        intent = parsed.get("intent", "general")
        suggestions = parsed.get("suggestions", ["Tìm sản phẩm", "Hỗ trợ"])

        if intent == "search_product":
            items = await core_client.search_items(
                q=parsed.get("query"),
                max_price=parsed.get("maxPrice"),
                min_price=parsed.get("minPrice"),
                condition=parsed.get("condition"),
                size=5,
            )

            products = _format_products(items)

            if products:
                # Gợi ý trong câu trả lời kèm link chi tiết
                product_links = "<ul>"
                for p in products:
                    product_links += f"<li><a href='/items/{p.item_id}' target='_blank' style='text-decoration: underline; color: #1677ff;'>{p.name}</a> - {p.price}</li>"
                product_links += "</ul>"
                
                reply = f"Mình tìm thấy <strong>{len(products)} sản phẩm phù hợp</strong> cho bạn! 🎉<br/>{product_links}"
            else:
                reply = "Mình chưa tìm thấy sản phẩm phù hợp. Bạn thử mô tả khác hoặc mở rộng tiêu chí nhé! 🔍"
                suggestions = ["Xem sản phẩm nổi bật", "Tìm với giá khác", "Hỗ trợ"]

            return ChatResponse(
                reply=reply,
                products=products,
                suggestions=suggestions,
                intent=intent,
            )

        reply = parsed.get("reply", "Mình hiểu rồi, bạn có cần giúp gì thêm không?")
        return ChatResponse(
            reply=reply,
            products=[],
            suggestions=suggestions,
            intent=intent,
        )

    except Exception as e:
        logger.error(f"Lỗi AI Chat: {e}", exc_info=True)
        return ChatResponse(
            reply="Xin lỗi, mình đang gặp sự cố kỹ thuật. Bạn vui lòng thử lại sau nhé! 🙏",
            products=[],
            suggestions=["Thử lại", "Tìm sản phẩm", "Hỗ trợ"],
            intent="error",
        )
