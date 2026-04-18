# 🔧 QUICK SETUP & INTEGRATION GUIDE

## ⚡5-Bước Quick Start

### Step 1: Database Migration (SQL)

```sql
-- File: backend/postgres/V8__create_history_tables.sql
-- Chạy SQL migration để tạo 2 bảng mới:
-- - search_history
-- - view_history

-- Verify:
SHOW TABLES LIKE '%history%';
```

### Step 2: Verify Entities & Repositories

✅ Models:

- `SearchHistory.java` - entity với @Table, @Index
- `ViewHistory.java` - entity với @ManyToOne reference

✅ Repositories:

- `SearchHistoryRepository.java` - JPA interface + custom queries
- `ViewHistoryRepository.java` - JPA interface + complex queries

### Step 3: Services Implementation

✅ Interfaces:

- `SearchHistoryService.java` - business logic
- `ViewHistoryService.java` - business logic

✅ Implementations:

- `SearchHistoryServiceImpl.java` - with @Transactional, cleanup
- `ViewHistoryServiceImpl.java` - with recommendations

### Step 4: Controllers & Endpoints

✅ `SearchHistoryController.java` - 8 endpoints
✅ `ViewHistoryController.java` - 10 endpoints

### Step 5: Enable Scheduler

Verify trong `@SpringBootApplication`:

```java
@EnableScheduling  // ✅ Phải có
public class CoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
```

---

## 📝 Manual Integration Points

### 1️⃣ Integration vào ItemController.getItemById()

**Vị trí file:** `backend/core-service/src/.../controller/ItemController.java`

**Thêm vào method:**

```java
// Thêm import
import com.secondhand.coreservice.service.ViewHistoryService;
import com.secondhand.coreservice.dto.request.ViewHistoryRequest;

// Inject service
@RequiredArgsConstructor
@RestController
public class ItemController {
    private final ViewHistoryService viewHistoryService;

    // ✅ Thêm vào getItemById (hoặc view item endpoint)
    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable String itemId) {
        ItemResponse item = itemService.getItemById(itemId);

        try {
            // Auto-save view history
            ViewHistoryRequest viewReq = ViewHistoryRequest.builder()
                .itemId(itemId)
                .sessionId(null)
                .build();
            viewHistoryService.saveViewHistory(viewReq);
        } catch (Exception e) {
            // Log but don't fail the request
            log.warn("Failed to save view history: {}", e.getMessage());
        }

        return ResponseEntity.ok(item);
    }
}
```

### 2️⃣ Integration vào Search/Filter Endpoint

**Tạo SearchController hoặc thêm vào ItemController:**

```java
// Thêm import
import com.secondhand.coreservice.service.SearchHistoryService;
import com.secondhand.coreservice.dto.request.SearchHistoryRequest;

// ✅ Thêm vào search method
@GetMapping("/search")
public ResponseEntity<List<ItemResponse>> searchItems(
        @RequestParam String query,
        @RequestParam(required = false) String categoryId) {

    List<ItemResponse> results = itemService.searchItems(query, categoryId);

    try {
        // Auto-save search history
        SearchHistoryRequest searchReq = SearchHistoryRequest.builder()
            .searchQuery(query)
            .categoryId(categoryId)
            .resultCount(results.size())
            .build();
        searchHistoryService.saveSearchHistory(searchReq);
    } catch (Exception e) {
        log.warn("Failed to save search history: {}", e.getMessage());
    }

    return ResponseEntity.ok(results);
}
```

### 3️⃣ React Frontend Integration

```javascript
// Lấy recommendations khi user nhìn thấy
useEffect(() => {
  fetch("/api/view-history/recommendations?limit=5", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  })
    .then((res) => res.json())
    .then((recommendations) => setRecommendations(recommendations))
    .catch((err) => console.error("Error:", err));
}, [token]);

// Lấy gợi ý tìm kiếm
function SearchSuggestions() {
  const [suggestions, setSuggestions] = useState([]);

  useEffect(() => {
    fetch("/api/search-history/suggestions", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then((data) => setSuggestions(data));
  }, [token]);

  return (
    <div className="suggestions">
      {suggestions.map((s) => (
        <div key={s} onClick={() => handleSearch(s)}>
          {s}
        </div>
      ))}
    </div>
  );
}
```

---

## 🧪 Testing Endpoints

### Test 1: Save View History

```bash
curl -X POST http://localhost:8080/api/view-history \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "itemId": "ITM12345678901234",
    "sessionId": "SESSION123"
  }'

Expected: 201 Created
{
  "id": 1,
  "itemId": "ITM12345678901234",
  "viewedAt": "2024-04-18T10:30:00",
  "sessionId": "SESSION123",
  "viewCount": 1
}
```

### Test 2: Get Recent Views

```bash
curl -X GET http://localhost:8080/api/view-history/recent \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

Expected: 200 OK
[ ViewHistoryResponse... ]
```

### Test 3: Get Recommendations

```bash
curl -X GET http://localhost:8080/api/view-history/recommendations?limit=10 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

Expected: 200 OK
[ ItemResponse... ]
```

### Test 4: Save Search History

```bash
curl -X POST http://localhost:8080/api/search-history \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "searchQuery": "iphone 13",
    "categoryId": "CELL001",
    "resultCount": 25
  }'

Expected: 201 Created
{
  "id": 1,
  "searchQuery": "iphone 13",
  "categoryId": "CELL001",
  "resultCount": 25,
  "createdAt": "2024-04-18T10:30:00"
}
```

### Test 5: Get Search Suggestions

```bash
curl -X GET http://localhost:8080/api/search-history/suggestions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

Expected: 200 OK
[
  "iphone 13",
  "áo thun",
  "laptop"
]
```

---

## 🔍 Database Verification

```sql
-- Verify tables created
SHOW TABLES LIKE '%history%';

-- Check search history data
SELECT COUNT(*) FROM search_history;
SELECT user_id, search_query, created_at FROM search_history LIMIT 5;

-- Check view history data
SELECT COUNT(*) FROM view_history;
SELECT user_id, item_id, viewed_at FROM view_history LIMIT 5;

-- Check indexes
SHOW INDEX FROM search_history;
SHOW INDEX FROM view_history;

-- Monitor table size
SELECT
  table_name,
  ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES
WHERE table_name IN ('search_history', 'view_history');
```

---

## 🚀 Deployment Checklist

### Pre-Deployment

- [ ] All tests passing
- [ ] Code review completed
- [ ] Documentation reviewed
- [ ] Database migration script tested locally

### During Deployment

- [ ] Run SQL migration: `V8__create_history_tables.sql`
- [ ] Verify tables created
- [ ] Deploy application
- [ ] Verify services start without error

### Post-Deployment

- [ ] Test all endpoints with JWT token
- [ ] Check application logs for errors
- [ ] Monitor database growth
- [ ] Verify scheduler running (check logs at 2 AM)
- [ ] Test cleanup scheduler (can manually call for testing)

### Monitoring

- [ ] Watch for slow queries (add to database monitoring)
- [ ] Monitor table size growth
- [ ] Check cleanup job executions
- [ ] Monitor API response times

---

## 📊 Expected Performance

### Query Performance

- `getRecentSearches()` - ~10ms (cached index)
- `getViewHistory()` - ~50ms (pagination 10 items)
- `getMostViewedItems()` - ~200ms (aggregation query)
- `getRecommendations()` - ~300ms (join + aggregation)

### Storage

- Per user per day: ~100 views + 10 searches = ~500 bytes
- 10,000 users per year: ~1.8 GB
- 90-day retention: ~450 MB

### Cleanup Job

- Duration: ~5-10 seconds (90-day cleanup)
- Frequency: Once daily at 2 AM
- Impact: Low (off-peak hours)

---

## 🐛 Troubleshooting

### Issue: "Unauthorized" on history endpoints

**Solution:**

- Check JWT token is valid
- Verify `getCurrentUserId()` works
- Check SecurityContext is populated

### Issue: No data showing in history

**Solution:**

- Verify tables are created: `SHOW TABLES LIKE '%history%'`
- Check integration points (Step 2 in Integration)
- Verify ViewHistoryService.saveViewHistory() is called

### Issue: Slow queries on history endpoints

**Solution:**

- Check indexes are created: `SHOW INDEX FROM view_history`
- Verify pagination is used (not loading all records)
- Monitor table size growth

### Issue: Scheduler not running

**Solution:**

- Verify `@EnableScheduling` in main application
- Check logs for "Starting history cleanup scheduler"
- May need to wait until next scheduled time (2 AM)

---

## 📚 Files Created/Modified

### New Files

- `SearchHistory.java` - Entity
- `ViewHistory.java` - Entity
- `SearchHistoryRepository.java` - Repository
- `ViewHistoryRepository.java` - Repository
- `SearchHistoryService.java` - Interface
- `ViewHistoryService.java` - Interface
- `SearchHistoryServiceImpl.java` - Implementation
- `ViewHistoryServiceImpl.java` - Implementation
- `SearchHistoryController.java` - Controller
- `ViewHistoryController.java` - Controller
- `HistoryCleanupScheduler.java` - Scheduler
- `V8__create_history_tables.sql` - Migration
- `SEARCH_VIEW_HISTORY_GUIDE.md` - Documentation

### Integration Points (Manual)

- `ItemController.java` - Add viewHistory save
- `SearchController.java` (if exists) - Add searchHistory save

---

## ✅ Verification Checklist

- [ ] All 13 files created successfully
- [ ] No compilation errors
- [ ] Database migration runs without error
- [ ] Application starts successfully
- [ ] All endpoints respond with 200/201
- [ ] Authentication works (JWT validation)
- [ ] Pagination works correctly
- [ ] Scheduler shows in logs
- [ ] Data appears in database
- [ ] Performance within expected range

---

**Status:** ✅ Ready for integration and testing
**Last Updated:** April 18, 2024
