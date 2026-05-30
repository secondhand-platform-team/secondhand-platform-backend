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
from app.models.schemas import ChatResponse, ProductResult, ActionItem
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
- Hỗ trợ đăng tin bán đồ cũ bằng cách phân tích thông tin mô tả sản phẩm
- Hỗ trợ hướng dẫn và định hướng người dùng đến các trang chức năng (như đổi mật khẩu, quản lý ví, giỏ hàng, thông tin cá nhân)
- Trả lời các câu hỏi về nền tảng ReLife

LUẬT:
- Trả lời bằng tiếng Việt, thân thiện và ngắn gọn
- Luôn trả về một object JSON hợp lệ theo đúng định dạng sau.

KHI USER MUỐN TÌM SẢN PHẨM:
{
  "intent": "search_product",
  "query": "từ khóa tìm kiếm (phải thật NGẰN GỌN và TỔNG QUÁT, ví dụ: 'bếp' thay vì 'bếp lò', 'điện thoại' thay vì 'điện thoại cũ')",
  "maxPrice": null hoặc số (VND),
  "minPrice": null hoặc số (VND),
  "condition": null hoặc "NEW" | "LIKE_NEW" | "USED",
  "category_hint": "gợi ý danh mục",
  "suggestions": ["Gợi ý 1", "Gợi ý 2"]
}

KHI USER MUỐN ĐĂNG BÁN ĐỒ CŨ (Ví dụ: "Tôi muốn bán cái điện thoại iPhone 11 cũ giá 4 triệu rưỡi ở Cầu Giấy, Hà Nội"):
{
  "intent": "create_listing",
  "title": "Tên sản phẩm bóc tách được (ví dụ: 'iPhone 11')",
  "price": Số tiền bóc tách được (ví dụ: 4500000) hoặc null nếu không rõ,
  "condition": "NEW" | "LIKE_NEW" | "USED" (Dựa vào độ cũ/mới người dùng mô tả, mặc định 'USED'),
  "category_hint": "Gợi ý danh mục phù hợp nhất từ danh sách sau: 'Đồ điện tử', 'Nhà cửa & đời sống', 'Thời trang & làm đẹp', 'Giải trí & sở thích', 'Bếp, lò, đồ điện nhà bếp', 'Cây cảnh, đồ trang trí', 'Điện thoại', 'Đồ gia dụng, nội thất, cây cảnh'",
  "city": "Tên tỉnh/thành phố bóc tách được. BẮT BUỘC phải chuẩn hóa có tiền tố 'Thành phố' hoặc 'Tỉnh' và viết hoa đúng chính tả (ví dụ: 'Thành phố Hà Nội', 'Tỉnh Lâm Đồng', 'Thành phố Hồ Chí Minh')",
  "district": "Tên quận/huyện bóc tách được. BẮT BUỘC phải chuẩn hóa có tiền tố 'Quận', 'Huyện', hoặc 'Thị xã' và viết hoa đúng chính tả (ví dụ: 'Quận Cầu Giấy', 'Quận 1', 'Huyện Đông Anh')",
  "description": "Mô tả ngắn gọn về tình trạng máy người dùng nêu (ví dụ: 'máy còn dùng tốt')",
  "reply": "Câu trả lời thân thiện thông báo bạn đã ghi nhận thông tin đăng bán và chuẩn bị tự động chuyển hướng người dùng sang trang đăng tin",
  "suggestions": ["Mở trang đăng tin ngay", "Hỏi ReBot câu khác"]
}

KHI USER MUỐN THỰC HIỆN CÁC THAO TÁC HỆ THỐNG / CHUYỂN TRANG (Ví dụ: "Hãy giúp tôi đổi mật khẩu", "Tôi muốn xem ví", "Xem giỏ hàng", "Đổi thông tin cá nhân"):
{
  "intent": "navigate",
  "target_page": "Đường dẫn đích (Chỉ được chọn 1 trong các đường dẫn sau: '/dashboard/password' (đổi mật khẩu), '/dashboard/profile' (thông tin cá nhân), '/dashboard/wallet' (ví cá nhân), '/dashboard/orders' (đơn hàng), '/dashboard/my-posts' (tin đã đăng), '/dashboard/favorites' (tin đã lưu), '/cart' (giỏ hàng), '/post-new' (đăng bán đồ cũ))",
  "reply": "Câu trả lời thông báo bạn đang chuyển hướng người dùng đến giao diện chức năng đó ngay lập tức (ví dụ: 'ReBot sẽ đưa bạn đến trang Đổi mật khẩu trong giây lát...')",
  "suggestions": ["Đến ngay", "Hỏi ReBot câu khác"]
}

KHI USER MUỐN THÊM VÀO GIỎ HÀNG (Ví dụ: "Thêm cái này vào giỏ", "Giúp tôi thêm vào giỏ hàng", "Tìm và thêm vào giỏ hàng cho tôi 3 đôi giày thể thao"):
{
  "intent": "add_to_cart",
  "scope": "current" (Nếu thêm sản phẩm đang xem hiện tại) hoặc "search" (Nếu muốn tìm kiếm và thêm hàng loạt sản phẩm mới),
  "query": "Từ khóa tìm kiếm (Ví dụ: 'giày thể thao') chỉ điền khi scope là 'search', ngược lại điền null",
  "quantity": Số lượng cần thêm (Ví dụ: 3) chỉ điền khi scope là 'search', ngược lại điền 1 hoặc null,
  "reply": "Câu trả lời thân thiện thông báo bạn đang thêm sản phẩm vào giỏ hàng...",
  "suggestions": ["Xem giỏ hàng", "Hỏi ReBot câu khác"]
}

KHI USER MUỐN MUA NGAY / THANH TOÁN (Ví dụ: "Mua ngay cái này", "Giúp tôi mua cái giày ở giao diện hiện tại"):
{
  "intent": "buy_now",
  "scope": "current" (Mặc định 'current'),
  "reply": "Câu trả lời thân thiện thông báo bạn đang thêm sản phẩm vào giỏ và đưa người dùng đến trang thanh toán...",
  "suggestions": ["Thanh toán ngay", "Hỏi ReBot câu khác"]
}

KHI USER MUỐN YÊU THÍCH SẢN PHẨM (Ví dụ: "Thêm sản phẩm này vào yêu thích", "Yêu thích cái này giúp tôi", "Tìm và yêu thích cho tôi 5 sản phẩm áo khoác"):
{
  "intent": "favorite",
  "scope": "current" (Nếu yêu thích sản phẩm hiện tại) hoặc "search" (Nếu muốn tìm và yêu thích sản phẩm mới),
  "query": "Từ khóa tìm kiếm sản phẩm cần yêu thích chỉ điền khi scope là 'search', ngược lại điền null",
  "quantity": Số lượng cần yêu thích (Ví dụ: 5) chỉ điền khi scope là 'search', ngược lại điền 1 hoặc null,
  "reply": "Câu trả lời thân thiện thông báo bạn đang yêu thích sản phẩm này...",
  "suggestions": ["Xem tin đã lưu", "Hỏi ReBot câu khác"]
}

KHI USER MUỐN XOÁ TRỐNG / DỌN DẸP GIỎ HÀNG (Ví dụ: "xoá trống giỏ hàng giúp tôi", "dọn sạch giỏ hàng", "xoá giỏ hàng"):
{
  "intent": "clear_cart",
  "reply": "Câu trả lời thân thiện thông báo bạn đang bắt đầu dọn dẹp và xoá trống giỏ hàng...",
  "suggestions": ["Xem giỏ hàng", "Hỏi ReBot câu khác"]
}

KHI USER MUỐN XOÁ KHỎI YÊU THÍCH / BỎ YÊU THÍCH SẢN PHẨM (Ví dụ: "bỏ yêu thích sản phẩm này", "xoá khỏi yêu thích cái này", "bỏ lưu sản phẩm giày reebok", "xoá giày thể thao khỏi yêu thích"):
{
  "intent": "unfavorite",
  "scope": "current" (Nếu bỏ yêu thích sản phẩm đang xem hiện tại) hoặc "search" (Nếu muốn tìm kiếm và bỏ yêu thích sản phẩm),
  "query": "Từ khóa tìm kiếm sản phẩm cần bỏ yêu thích chỉ điền khi scope là 'search', ngược lại điền null",
  "quantity": Số lượng cần bỏ yêu thích (Ví dụ: 1) chỉ điền khi scope là 'search', ngược lại điền 1 hoặc null,
  "reply": "Câu trả lời thân thiện thông báo bạn đang thực hiện bỏ yêu thích sản phẩm...",
  "suggestions": ["Xem tin đã lưu", "Hỏi ReBot câu khác"]
}

KHI USER HỎI VỀ NỀN TẢNG HOẶC TRÒ CHUYỆN CHUNG:
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

        if intent == "create_listing":
            return ChatResponse(
                reply=parsed.get("reply", "Mình sẽ chuyển bạn sang trang đăng tin trong giây lát để tự động điền các thông tin nhé..."),
                products=[],
                suggestions=suggestions,
                intent=intent,
                title=parsed.get("title"),
                price=parsed.get("price"),
                condition=parsed.get("condition"),
                city=parsed.get("city"),
                district=parsed.get("district"),
                description=parsed.get("description"),
                category_hint=parsed.get("category_hint"),
            )

        if intent in ["add_to_cart", "buy_now", "favorite", "unfavorite"]:
            scope = parsed.get("scope", "current")
            query = parsed.get("query")
            quantity = parsed.get("quantity") or 1
            
            action_items = []
            reply = parsed.get("reply", "Mình đang thực hiện yêu cầu của bạn...")
            
            if scope == "search" and query:
                # Call search catalog from core backend
                items = await core_client.search_items(
                    q=query,
                    size=quantity
                )
                action_items = [
                    ActionItem(item_id=item.get("itemId", ""), title=item.get("title", "Sản phẩm"))
                    for item in items
                ]
                if not action_items:
                    reply = f"Mình tìm kiếm sản phẩm '{query}' để thực hiện yêu cầu nhưng rất tiếc là chưa thấy sản phẩm nào đang rao bán trên hệ thống. 🔍"
                else:
                    item_names = ", ".join([f"<strong>{item.title}</strong>" for item in action_items])
                    if intent == "add_to_cart":
                        action_verb = "giỏ hàng"
                        reply = f"ReBot đã tìm thấy và tự động thêm các sản phẩm sau vào {action_verb} của bạn: {item_names}! 🛒✨"
                    elif intent == "favorite":
                        action_verb = "danh sách yêu thích"
                        reply = f"ReBot đã tìm thấy và tự động thêm các sản phẩm sau vào {action_verb} của bạn: {item_names}! ❤️✨"
                    elif intent == "unfavorite":
                        action_verb = "xoá khỏi danh sách yêu thích"
                        reply = f"ReBot đã tìm thấy và tự động xoá các sản phẩm sau khỏi {action_verb} của bạn: {item_names}! 💔✨"
            
            return ChatResponse(
                reply=reply,
                products=[],
                suggestions=suggestions,
                intent=intent,
                scope=scope,
                quantity=quantity,
                action_items=action_items
            )

        if intent == "clear_cart":
            return ChatResponse(
                reply=parsed.get("reply", "ReBot đang chuẩn bị xoá trống giỏ hàng giúp bạn nhé... 🛒"),
                products=[],
                suggestions=suggestions,
                intent=intent,
            )

        if intent == "navigate":
            return ChatResponse(
                reply=parsed.get("reply", "Đang chuyển hướng bạn đến giao diện yêu cầu..."),
                products=[],
                suggestions=suggestions,
                intent=intent,
                target_page=parsed.get("target_page"),
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
