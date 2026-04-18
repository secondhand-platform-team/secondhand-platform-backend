# 📊 SEARCH & VIEW HISTORY FEATURE - IMPLEMENTATION GUIDE

## 🎯 Tổng Quan

Hệ thống lịch sử tìm kiếm và xem tin tối ưu cho secondhand platform. Giúp user:

- 🔍 Xem lại các tìm kiếm trước đó
- 👁️ Xem lại các tin đã xem
- 💡 Nhận được gợi ý dựa trên hành động
- 📈 Hỗ trợ analytics và UX improvement

---

## 📁 Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│ Controllers                                                 │
│ ├─ SearchHistoryController  (/api/search-history)         │
│ └─ ViewHistoryController    (/api/view-history)           │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ Services                                                    │
│ ├─ SearchHistoryService / Impl                            │
│ └─ ViewHistoryService / Impl                              │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ Repositories                                                │
│ ├─ SearchHistoryRepository  (custom JPA queries)          │
│ └─ ViewHistoryRepository    (complex queries)             │
└──────────────────┬──────────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────────┐
│ Database                                                    │
│ ├─ search_history   (indexed)                             │
│ └─ view_history     (indexed)                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Database Schema

### search_history Table

```sql
search_history
├─ id (BIGINT, PK, auto increment)
├─ user_id (VARCHAR 255, FK, NOT NULL)
├─ search_query (TEXT, NOT NULL)
├─ category_id (VARCHAR 255, optional)
├─ result_count (INT, optional)
└─ created_at (DATETIME, NOT NULL)

Indexes:
├─ idx_user_id_created_at (user_id DESC, created_at DESC)
├─ idx_user_id
└─ idx_created_at DESC
```

**Mục đích lưu trữ:**

- Từ khóa tìm kiếm của user
- Phục vụ gợi ý tìm kiếm
- Analytics
- Trending searches

**Data Retention:** 90 ngày (tự động xóa qua scheduler)

---

### view_history Table

```sql
view_history
├─ id (BIGINT, PK, auto increment)
├─ user_id (VARCHAR 255, NOT NULL)
├─ item_id (VARCHAR 255, FK → items, NOT NULL)
├─ viewed_at (DATETIME, NOT NULL)
├─ session_id (VARCHAR 255, optional)
└─ created_at (DATETIME, NOT NULL)

Indexes:
├─ idx_user_id_item_id_created_at (user_id DESC, item_id, created_at DESC)
├─ idx_user_id_created_at (user_id DESC, created_at DESC)
├─ idx_item_id_created_at (item_id, created_at DESC)
├─ idx_item_id
└─ idx_session_id
```

**Mục đích lưu trữ:**

- Lần xem items của user
- Phục vụ gợi ý items (recommendations)
- Most viewed analytics
- Track user behavior

**Data Retention:** 90 ngày (tự động xóa qua scheduler)

**FK Relationship:** CASCADE DELETE (xóa item sẽ xóa view history)

---

## 🔌 API Endpoints

### 1. Search History Endpoints

#### POST /api/search-history

**Save search history**

```bash
Request:
POST /api/search-history
Content-Type: application/json
Authorization: Bearer <token>

{
  "searchQuery": "iphone 13",
  "categoryId": "CELL001",
  "resultCount": 25
}

Response: 201 Created
{
  "id": 1,
  "searchQuery": "iphone 13",
  "categoryId": "CELL001",
  "resultCount": 25,
  "createdAt": "2024-04-18T10:30:00"
}
```

#### GET /api/search-history

**Get user's search history (paginated)**

```bash
Request:
GET /api/search-history?page=0&size=10
Authorization: Bearer <token>

Response: 200 OK
{
  "content": [
    {
      "id": 5,
      "searchQuery": "áo thun nam",
      "categoryId": "CLOTH001",
      "resultCount": 15,
      "createdAt": "2024-04-18T15:00:00"
    },
    ...
  ],
  "pageable": {...},
  "totalElements": 45,
  "totalPages": 5
}
```

#### GET /api/search-history/recent

**Get 10 recent searches**

```bash
Request:
GET /api/search-history/recent
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 5,
    "searchQuery": "áo thun nam",
    ...
  },
  ...
]
```

#### GET /api/search-history/suggestions

**Get search suggestions (distinct queries)**

```bash
Request:
GET /api/search-history/suggestions
Authorization: Bearer <token>

Response: 200 OK
[
  "iphone 13",
  "áo thun nam",
  "laptop dell",
  "giày nike"
]
```

#### GET /api/search-history/trending

**Get trending searches (global)**

```bash
Request:
GET /api/search-history/trending
Authorization: Bearer <token>

Response: 200 OK
[
  "iphone 14",
  "macbook pro",
  "áo phông"
]
```

#### GET /api/search-history/category/{categoryId}

**Get searches by category**

```bash
Request:
GET /api/search-history/category/CELL001?page=0&size=10
Authorization: Bearer <token>

Response: 200 OK
{ Page<SearchHistoryResponse> }
```

#### DELETE /api/search-history/{id}

**Delete one search record**

```bash
Request:
DELETE /api/search-history/1
Authorization: Bearer <token>

Response: 200 OK
{
  "success": true,
  "message": "Search history deleted"
}
```

#### DELETE /api/search-history/clear

**Clear ALL search history**

```bash
Request:
DELETE /api/search-history/clear
Authorization: Bearer <token>

Response: 200 OK
{
  "success": true,
  "message": "Search history cleared"
}
```

---

### 2. View History Endpoints

#### POST /api/view-history

**Save view history (call when user views item detail)**

```bash
Request:
POST /api/view-history
Content-Type: application/json
Authorization: Bearer <token>

{
  "itemId": "ITM12345678901234",
  "sessionId": "SESSION123"
}

Response: 201 Created
{
  "id": 42,
  "itemId": "ITM12345678901234",
  "viewedAt": "2024-04-18T10:30:00",
  "sessionId": "SESSION123",
  "viewCount": 3
}
```

#### GET /api/view-history

**Get user's view history (paginated)**

```bash
Request:
GET /api/view-history?page=0&size=10
Authorization: Bearer <token>

Response: 200 OK
{
  "content": [
    {
      "id": 42,
      "itemId": "ITM12345678901234",
      "viewedAt": "2024-04-18T10:30:00",
      "viewCount": 3
    },
    ...
  ],
  "pageable": {...}
}
```

#### GET /api/view-history/recent

**Get 20 recently viewed items**

```bash
Request:
GET /api/view-history/recent
Authorization: Bearer <token>

Response: 200 OK
[ ViewHistoryResponse[] ]
```

#### GET /api/view-history/most-viewed

**Get most viewed items**

```bash
Request:
GET /api/view-history/most-viewed?page=0&size=10
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "itemId": "ITM001",
    "viewCount": 15
  },
  {
    "itemId": "ITM002",
    "viewCount": 12
  }
]
```

#### GET /api/view-history/recommendations

**Get recommendations (hot items in last 30 days)**

```bash
Request:
GET /api/view-history/recommendations?limit=10
Authorization: Bearer <token>

Response: 200 OK
[ ItemResponse[] ] // Items được xem nhiều nhất 30 ngày qua
```

#### GET /api/view-history/stats

**Get view statistics**

```bash
Request:
GET /api/view-history/stats
Authorization: Bearer <token>

Response: 200 OK
{
  "totalDistinctItemsViewed": 125,
  "recentlyViewedCount": 45
}
```

#### GET /api/view-history/check/{itemId}

**Check if user viewed item**

```bash
Request:
GET /api/view-history/check/ITM12345678901234
Authorization: Bearer <token>

Response: 200 OK
{
  "itemId": "ITM12345678901234",
  "viewed": true,
  "viewCount": 3
}
```

#### DELETE /api/view-history/{id}

**Delete one view record**

```bash
Request:
DELETE /api/view-history/42
Authorization: Bearer <token>

Response: 200 OK
{ MessageResponse }
```

#### DELETE /api/view-history/clear

**Clear ALL view history**

```bash
Request:
DELETE /api/view-history/clear
Authorization: Bearer <token>

Response: 200 OK
{ MessageResponse }
```

---

## 💡 Integration Guide

### 1. Auto-save View History

Trong `ItemController.getItemById()`, thêm:

```java
@GetMapping("/{itemId}")
public ResponseEntity<ItemResponse> getItemById(@PathVariable String itemId) {
    ItemResponse item = itemService.getItemById(itemId);

    // ✅ Auto-save view history
    ViewHistoryRequest req = ViewHistoryRequest.builder()
        .itemId(itemId)
        .sessionId(null) // Optional
        .build();
    viewHistoryService.saveViewHistory(req);

    return ResponseEntity.ok(item);
}
```

### 2. Auto-save Search History

Khi user tìm kiếm items, gọi:

```java
// Sau khi lấy kết quả search
List<ItemResponse> results = itemService.searchItems(query, categoryId);

// ✅ Save search history
SearchHistoryRequest req = SearchHistoryRequest.builder()
    .searchQuery(query)
    .categoryId(categoryId)
    .resultCount(results.size())
    .build();
searchHistoryService.saveSearchHistory(req);
```

### 3. Display Recommendations Frontend

```javascript
// JavaScript fetch recommendations
fetch("/api/view-history/recommendations?limit=10", {
  headers: {
    Authorization: `Bearer ${token}`,
  },
})
  .then((res) => res.json())
  .then((items) => displayRecommendations(items));
```

---

## ⚙️ Performance Optimization

### Indexing Strategy

```sql
-- Composite index cho queries phổ biến
INDEX idx_user_id_created_at (user_id DESC, created_at DESC)

-- Single index cho mỗi column
INDEX idx_user_id (user_id)
INDEX idx_item_id (item_id)
INDEX idx_created_at (created_at DESC)
```

**Benefit:**

- Query trên user → ✅ FAST
- Sort by created_at DESC → ✅ FAST
- JOIN với items table → ✅ FAST (covering index)

### Pagination

```java
// Không nên lấy toàn bộ dữ liệu
List<ViewHistory> all = viewHistoryRepository.findAll(); // ❌ BAD

// Luôn sử dụng pagination
Page<ViewHistory> page = viewHistoryRepository
    .findByUserIdOrderByViewedAtDesc(userId,
        PageRequest.of(0, 10)); // ✅ GOOD
```

### Data Retention

```java
// Auto cleanup cũ hơn 90 ngày
@Scheduled(cron = "0 0 2 * * *") // 2 AM mỗi ngày
public void cleanupOldHistories() {
    searchHistoryService.cleanupOldSearchHistory();
    viewHistoryService.cleanupOldViewHistory();
}
```

**Estimated Storage:**

- 10,000 users × 100 views/month = 12M records/year
- ~5-10GB per year (with indexes)
- 90-day retention = manageable storage

---

## 🔒 Security

### Authentication Required

Tất cả endpoints yêu cầu JWT token hợp lệ

```java
String userId = getCurrentUserId(); // From JWT
```

### Authorization Check

User chỉ có thể xem/xóa riêng history của mình

```java
if (!history.getUserId().equals(userId)) {
    throw new SecurityException("Unauthorized");
}
```

### Data Privacy

- Không trả về private info của users khác
- Trending searches không chứa PII (personalized)
- View count chỉ hiện per-user, không cross-user

---

## 🧪 Testing

### Unit Tests

```java
@Test
public void testSaveViewHistory() {
    ViewHistoryRequest req = ViewHistoryRequest.builder()
        .itemId("ITM001")
        .build();

    ViewHistoryResponse res = viewHistoryService.saveViewHistory(req);

    assertThat(res).isNotNull();
    assertThat(res.getItemId()).isEqualTo("ITM001");
}

@Test
public void testGetRecommendations() {
    List<ItemResponse> recs = viewHistoryService.getRecommendations(10);

    assertThat(recs).isNotEmpty();
    assertThat(recs.size()).isLessThanOrEqualTo(10);
}
```

### Integration Tests

```bash
# Save view history
curl -X POST http://localhost:8080/api/view-history \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "itemId": "ITM001",
    "sessionId": "SESSION123"
  }'

# Get recent views
curl -X GET http://localhost:8080/api/view-history/recent \
  -H "Authorization: Bearer <token>"

# Get recommendations
curl -X GET http://localhost:8080/api/view-history/recommendations?limit=10 \
  -H "Authorization: Bearer <token>"
```

---

## 📊 Monitoring & Analytics

### Key Metrics

1. **Search Performance**
   - Most searched keywords
   - Search trends over time
   - Search to view conversion rate

2. **View Performance**
   - Most viewed items
   - Average time on detail page
   - View distribution by category

3. **System Health**
   - History table size (monitor for growth)
   - Query performance (slow log)
   - Cleanup job status

### Queries

```sql
-- Top 20 trending searches (last 7 days)
SELECT search_query, COUNT(*) as count
FROM search_history
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY search_query
ORDER BY count DESC
LIMIT 20;

-- Most viewed items (all time)
SELECT item_id, COUNT(*) as views
FROM view_history
GROUP BY item_id
ORDER BY views DESC
LIMIT 20;

-- View history distribution by user
SELECT user_id, COUNT(*) as view_count
FROM view_history
GROUP BY user_id
ORDER BY view_count DESC;

-- Table size
SELECT
  table_name,
  ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES
WHERE table_name IN ('search_history', 'view_history');
```

---

## 🚀 Deployment Checklist

- [ ] Run SQL migration: `V8__create_history_tables.sql`
- [ ] Verify tables created: `SHOW TABLES LIKE '%history%'`
- [ ] Deploy services
- [ ] Enable @Scheduled annotation in main application
- [ ] Test endpoints with auth token
- [ ] Monitor cleanup job logs
- [ ] Set up database monitoring
- [ ] Document for frontend team

---

## 📝 Future Enhancements

1. **Advanced Recommendations**
   - Collaborative filtering
   - Content-based recommendations
   - ML-based suggestions

2. **Analytics Dashboard**
   - User behavior analytics
   - Search trend visualization
   - Category popularity

3. **Caching**
   - Redis cache for trending searches
   - Cache recommendations (TTL 1 hour)
   - Cache top viewed items

4. **Export**
   - Export search history
   - Export view history
   - CSV/PDF download

5. **Privacy Features**
   - Private browsing mode (no history)
   - Selective history deletion
   - Auto-delete on logout

---

## 📞 Support

- **Issues**: Check error logs
- **Debug**: Enable DEBUG logging for services
- **Questions**: Refer to API documentation above

**Last Updated:** April 2024
