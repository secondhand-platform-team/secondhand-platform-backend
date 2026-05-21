"""
HTTP Client gọi đến core-service (Spring Boot) qua Kong Gateway.
AI Service KHÔNG query DB trực tiếp — luôn gọi qua REST API.
"""
# pyrefly: ignore [missing-import]
import httpx
import logging
from typing import Optional
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

# Dùng chung 1 async client để tận dụng connection pooling
_client: Optional[httpx.AsyncClient] = None


async def get_http_client() -> httpx.AsyncClient:
    global _client
    if _client is None or _client.is_closed:
        _client = httpx.AsyncClient(timeout=httpx.Timeout(15.0))
    return _client


async def close_http_client():
    global _client
    if _client and not _client.is_closed:
        await _client.aclose()
        _client = None


async def search_items(
    q: Optional[str] = None,
    category_id: Optional[str] = None,
    min_price: Optional[int] = None,
    max_price: Optional[int] = None,
    condition: Optional[str] = None,
    transaction_type: Optional[str] = None,
    city: Optional[str] = None,
    size: int = 5,
) -> list[dict]:
    """
    Gọi core-service GET /api/items/search.
    Trả về danh sách sản phẩm dạng dict.
    """
    client = await get_http_client()
    params: dict = {"size": size}
    if q:
        params["q"] = q
    if category_id:
        params["categoryId"] = category_id
    if min_price is not None:
        params["minPrice"] = min_price
    if max_price is not None:
        params["maxPrice"] = max_price
    if condition:
        params["condition"] = condition
    if transaction_type:
        params["transactionType"] = transaction_type
    if city:
        params["city"] = city

    url = f"{settings.core_service_url}/api/items/search"
    try:
        resp = await client.get(url, params=params)
        resp.raise_for_status()
        data = resp.json()
        # Spring Boot Page object trả về { content: [...], ... }
        return data.get("content", [])
    except Exception as e:
        logger.error(f"Lỗi gọi core-service search: {e}")
        return []


async def get_categories() -> list[dict]:
    """Lấy danh sách category từ core-service."""
    client = await get_http_client()
    url = f"{settings.core_service_url}/api/categories"
    try:
        resp = await client.get(url)
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        logger.error(f"Lỗi gọi core-service categories: {e}")
        return []


async def get_featured_items(limit: int = 8) -> list[dict]:
    """Lấy sản phẩm nổi bật."""
    client = await get_http_client()
    url = f"{settings.core_service_url}/api/items/featured"
    try:
        resp = await client.get(url, params={"limit": limit})
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        logger.error(f"Lỗi gọi core-service featured: {e}")
        return []


async def get_item_by_id(item_id: str) -> Optional[dict]:
    """Lấy chi tiết 1 sản phẩm."""
    client = await get_http_client()
    url = f"{settings.core_service_url}/api/items/{item_id}"
    try:
        resp = await client.get(url)
        resp.raise_for_status()
        return resp.json()
    except Exception as e:
        logger.error(f"Lỗi gọi core-service item/{item_id}: {e}")
        return None
