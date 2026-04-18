# ✅ Items API - Khám Phá Hoàn Tất

## 📄 Tệp Tài Liệu Đã Tạo

Tôi đã tạo **4 tệp tài liệu** chi tiết trong thư mục `backend/`:

### 1. 📋 **ITEMS_API_QUICK_SUMMARY.md** ⭐ START HERE

- Tóm tắt nhanh toàn bộ API
- Danh sách tất cả endpoints
- Enum values & validation rules
- Ví dụ CURL cơ bản
- File structure
- **Đây là tệp đầu tiên bạn nên đọc**

### 2. 📚 **ITEMS_API_DOCUMENTATION.md**

- Tài liệu API chi tiết hoàn chỉnh
- Mô tả chi tiết từng endpoint (GET, POST, PUT, PATCH, DELETE)
- Request/Response format đầy đủ
- Database schema của Item entity
- Mối quan hệ (relationships)
- Validation rules
- Authentication & Authorization
- Payment flow
- ~500+ dòng thông tin chi tiết

### 3. 🔧 **ITEMS_API_IMPLEMENTATION.md**

- Chi tiết về Repository layer
- Service implementation logic
- DTO mapping
- Enums & Entity models
- Transaction management
- External integrations (CloudinaryService, PaymentEventService, UserServiceClient)
- Performance considerations
- ~400+ dòng về implementation

### 4. 💻 **ITEMS_API_EXAMPLES.md**

- CURL examples cho tất cả endpoints
- Request/Response examples chi tiết
- VNPay payment callback examples
- Postman collection JSON
- Common error responses
- ~600+ dòng examples

---

## 🎯 Nội Dung Chính Đã Khám Phá

### ✅ Controllers

```
ItemController.java
├── @PostMapping → createItem() / createItemJson()
├── @GetMapping → getAllItems() / getMyItems() / getItemById() / etc.
├── @PutMapping → updateItem()
├── @PatchMapping → updateItemStatus()
├── @DeleteMapping → deleteItem()
└── @PostMapping/DeleteMapping → addFavorite() / removeFavorite()
```

### ✅ DTOs

**Request:**

- ItemRequest
- ItemStatusUpdateRequest
- ItemImageRequest
- ItemAttributeRequest
- LocationRequest
- VNPayCallbackRequest

**Response:**

- ItemResponse
- ItemImageResponse
- LocationResponse
- ItemAttributeResponse
- MessageResponse

### ✅ Services

- ItemService (interface)
- ItemServiceImpl (implementation)
  - createItem() - tạo tin
  - getAllItems() - lấy tất cả
  - getMyItems() - tin của tôi
  - getItemById() - lấy chi tiết
  - getItemsByCategory() / getItemsByCategorySlug()
  - getItemsByUser()
  - updateItem() - cập nhật toàn bộ
  - updateItemStatus() - cập nhật status
  - deleteItem() - xóa tin
  - addFavoriteItem() / removeFavoriteItem()
  - getMyFavoriteItems()
  - handleVNPayCallback()

### ✅ Models & Entities

```
Item (Main Entity)
├── itemId (ITM + 14 digits)
├── title (bắt buộc)
├── description
├── price (bắt buộc)
├── transactionType (SELL / GIVE_AWAY)
├── condition (NEW / LIKE_NEW / USED / FOR_PARTS)
├── status (AVAILABLE / RESERVED / SOLD / HIDDEN / ACTIVE / DRAFT / EXPIRED)
├── userId (chủ sở hữu)
├── categoryId (FK)
├── createdAt (tự set)
├── updatedAt (tự update)
├── transactionId (VNPay)
├── paymentUrl (VNPay)
├── paymentInitiatedAt (VNPay timeout)
└── Relationships:
    ├── location (Location 1:1)
    ├── itemImageList (ItemImage 1:N)
    ├── attributeValues (ItemAttributeValue 1:N)
    ├── category (Category N:1)
    ├── favoriteItems (FavoriteItem 1:N)
    ├── reviewList (Review 1:N)
    ├── giveawayRequestList (GiveawayRequest 1:N)
    ├── reports (Report 1:N)
    └── notifications (Notification 1:N)
```

### ✅ Repositories

- ItemRepository
  - findByItemId()
  - findByUserId()
  - findByCategory_CategoryId()
  - findByCategory_SlugAndStatusIn()
- FavoriteItemRepository
  - findByUserId()
  - findByUserIdAndItem_ItemId()
  - existsByUserIdAndItem_ItemId()

### ✅ Enums

```
ItemStatus: AVAILABLE, RESERVED, SOLD, HIDDEN, ACTIVE, DRAFT, EXPIRED
ItemCondition: NEW, LIKE_NEW, USED, FOR_PARTS
TransactionType: SELL, GIVE_AWAY
```

---

## 📊 Tóm Tắt API

### Endpoints (14 endpoints)

| Method | Endpoint                        | Mô Tả                | Auth |
| ------ | ------------------------------- | -------------------- | ---- |
| POST   | /api/items                      | Tạo tin              | ✅   |
| POST   | /api/items/json                 | Tạo tin (JSON)       | ✅   |
| GET    | /api/items                      | Lấy tất cả           | ❌   |
| GET    | /api/items/me                   | Tin của tôi          | ✅   |
| GET    | /api/items/{id}                 | Chi tiết             | ❌   |
| GET    | /api/items/category/{id}        | Theo danh mục (ID)   | ❌   |
| GET    | /api/items/category/slug/{slug} | Theo danh mục (Slug) | ❌   |
| GET    | /api/items/user/{id}            | Tin của user         | ❌   |
| GET    | /api/items/favorites/me         | Danh sách yêu thích  | ✅   |
| PUT    | /api/items/{id}                 | Cập nhật tin         | ✅   |
| PATCH  | /api/items/{id}/status          | Cập nhật status      | ✅   |
| DELETE | /api/items/{id}                 | Xóa tin              | ✅   |
| POST   | /api/items/{id}/favorite        | Thêm yêu thích       | ✅   |
| DELETE | /api/items/{id}/favorite        | Xóa yêu thích        | ✅   |

### Key Features ✨

1. **Create (CRUD)**
   - Hỗ trợ multipart upload ảnh + JSON
   - Hỗ trợ JSON thuần
   - Auto upload ảnh lên Cloudinary
   - Validate dữ liệu
   - Tích hợp VNPay thanh toán
   - Tự sinh ID (ITM + 14 digits)

2. **Read (CRUD)**
   - Lấy tất cả / tin của tôi / theo ID
   - Lọc theo danh mục (ID/Slug)
   - Lấy tin của user khác
   - Lấy danh sách yêu thích

3. **Update (CRUD)**
   - Cập nhật toàn bộ thông tin
   - Cập nhật riêng status
   - Cập nhật location, images, attributes
   - Chỉ chủ sở hữu có quyền
   - Auto update timestamp

4. **Delete (CRUD)**
   - Xóa tin hoàn toàn
   - Cascade delete (images, attributes)
   - Chỉ chủ sở hữu có quyền

5. **Favorite Management**
   - Thêm/xóa yêu thích
   - Lấy danh sách yêu thích
   - Kiểm tra trùng lặp tự động

6. **Payment Integration**
   - Tạo VNPay payment URL
   - Xử lý VNPay callback
   - Xác thực secure hash
   - Auto status update khi thành công

---

## 📦 Database Schema

### Bảng Chính: `items`

```
item_id (VARCHAR 20) - Primary Key
title (TEXT) - Bắt buộc
description (TEXT)
price (DECIMAL 19,2) - Bắt buộc
transaction_type (ENUM) - SELL, GIVE_AWAY
condition (ENUM) - NEW, LIKE_NEW, USED, FOR_PARTS
status (ENUM) - ACTIVE, DRAFT, etc.
view (INT)
created_at (DATETIME) - Auto set
updated_at (DATETIME) - Auto update
user_id (VARCHAR 20) - Foreign key
category_id (VARCHAR 20) - Foreign key
transaction_id (VARCHAR 50) - VNPay
payment_url (TEXT)
payment_initiated_at (DATETIME)
```

### Bảng Liên Quan:

- item_images (1:N)
- item_attribute_values (1:N)
- locations (1:1)
- favorite_items (1:N)
- categories (N:1)
- reviews (1:N)
- giveaway_requests (1:N)
- reports (1:N)
- notifications (1:N)

---

## 🔐 Authentication & Authorization

```
Authentication: JWT Bearer Token
Required for: Create, Update, Delete, Favorites operations
Authorization:
- Create: Any logged-in user
- Read: Public (no auth)
- Update: Item owner only
- Delete: Item owner only
- Favorites: Any logged-in user
```

---

## 💳 Payment Flow (SELL Items)

```
1. User POST /api/items
   ↓
2. System creates VNPay payment
   - Status = DRAFT
   - Returns paymentUrl
   ↓
3. User attends paymentUrl
   ↓
4. User pays on VNPay
   ↓
5. VNPay callback → /payment-callback
   ↓
6. System verifies callback
   - If success: Status = ACTIVE
   - If fail: Status = DRAFT or deleted
```

**Timeout: 15 minutes**

---

## 📂 Project Structure

```
backend/core-service/
├── controller/
│   └── ItemController.java
├── service/
│   ├── ItemService.java
│   └── impl/ItemServiceImpl.java
├── dto/
│   ├── request/ (ItemRequest, ItemStatusUpdateRequest, etc.)
│   └── response/ (ItemResponse, ItemImageResponse, etc.)
├── model/
│   ├── Item.java
│   ├── ItemImage.java
│   ├── ItemAttributeValue.java
│   ├── Location.java
│   ├── FavoriteItem.java
│   └── enums/ (ItemStatus, ItemCondition, TransactionType)
├── repository/
│   ├── ItemRepository.java
│   └── FavoriteItemRepository.java
└── utils/
    └── IdGenerator.java
```

---

## 🚀 Quick Start

### 1. Tạo Tin Đăng SELL

```bash
curl -X POST "http://localhost:8080/api/items" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -F "item={...}" \
  -F "images=@image1.jpg"
```

**Response**: paymentUrl + transactionId

### 2. Lấy Tất Cả Tin

```bash
curl -X GET "http://localhost:8080/api/items"
```

**Response**: List<ItemResponse>

### 3. Cập Nhật Tin

```bash
curl -X PUT "http://localhost:8080/api/items/ITM00000000000001" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -d '{...}'
```

**Response**: ItemResponse

### 4. Xóa Tin

```bash
curl -X DELETE "http://localhost:8080/api/items/ITM00000000000001" \
  -H "Authorization: Bearer JWT_TOKEN"
```

**Response**: {message, success}

---

## ✅ Request Validation

```
ItemRequest:
✅ title - Not blank (bắt buộc)
✅ categoryId - Not blank, phải tồn tại
✅ price - Not null
  - SELL: price > 0
  - GIVE_AWAY: price >= 0
📝 condition - NEW, LIKE_NEW, USED, FOR_PARTS
📝 transactionType - SELL, GIVE_AWAY
📝 status - ACTIVE, DRAFT, AVAILABLE, RESERVED, SOLD, HIDDEN, EXPIRED
📷 images - JPG, PNG, GIF, WebP, max 10MB
```

---

## 🎯 Quy Luật Kinh Doanh

### SELL Items (Bán)

- Yêu cầu thanh toán VNPay
- Status: DRAFT (chờ) → ACTIVE (đã thanh toán)
- Price: Phải > 0
- PaymentUrl: Được tạo tự động
- PaymentTimeout: 15 phút

### GIVE_AWAY Items (Cho Tặng)

- Không cần thanh toán
- Status: ACTIVE ngay lập tức
- Price: Có thể = 0 hoặc > 0
- PaymentUrl: null
- PaymentTimeout: N/A

---

## 📈 Performance & Optimization

✅ **Lazy Loading**: Category, Location lazy load  
✅ **Cascade Delete**: Xóa tin tự động xóa images, attributes  
✅ **Transaction Management**: @Transactional trên service methods  
✅ **Error Handling**: Custom exceptions (BadRequestException, ResourceNotFoundException)  
✅ **Logging**: Sử dụng SLF4J (@Slf4j)

### Cân nhắc khi tối ưu:

- Thêm pagination (Pageable) cho GET endpoints
- Thêm database indexes (userId, categoryId, status)
- Implement caching cho categories/attributes
- Join fetch queries nếu cần
- Batch image upload tới Cloudinary

---

## 🔗 External Integrations

### 1. Cloudinary

- Lưu trữ & serve ảnh
- Upload: 10MB max per file
- Formats: JPG, PNG, GIF, WebP
- Return: Image URL

### 2. VNPay

- Payment gateway
- Create payment: TransactionId + PaymentUrl
- Callback: Verify + Update status
- Secure hash verification

### 3. UserService (Auth-Service)

- Verify user via JWT
- Get user details
- Integration: Feign/gRPC

---

## 📚 Tệp Tham Khảo Khác

```
backend/
├── ITEMS_API_QUICK_SUMMARY.md ← START HERE (90 dòng)
├── ITEMS_API_DOCUMENTATION.md (500+ dòng)
├── ITEMS_API_IMPLEMENTATION.md (400+ dòng)
└── ITEMS_API_EXAMPLES.md (600+ dòng)
```

---

## 🎓 Cách Sử Dụng Tài Liệu

### Nếu bạn muốn:

- **Hiểu nhanh overview** → ITEMS_API_QUICK_SUMMARY.md ⭐
- **Tìm endpoint cụ thể** → ITEMS_API_DOCUMENTATION.md
- **Hiểu code implementation** → ITEMS_API_IMPLEMENTATION.md
- **Test API bằng CURL** → ITEMS_API_EXAMPLES.md
- **Biết database schema** → Cả 3 tệp đều có (xem DOCUMENTATION)

---

## 🔍 Key Takeaways

| Khía Cạnh          | Chi Tiết                                      |
| ------------------ | --------------------------------------------- |
| **Endpoints**      | 14 endpoints (CRUD + Favorites + Payment)     |
| **Entity**         | Item với 15+ fields                           |
| **Status Choices** | 7 enum values                                 |
| **Auth**           | JWT Token (write/delete required)             |
| **Payment**        | VNPay integration (SELL items)                |
| **Images**         | Cloudinary upload (10MB max)                  |
| **ID Format**      | ITM + 14 digits (auto-generated)              |
| **Relationships**  | 9 related entities (images, attributes, etc.) |
| **Features**       | CRUD + Favorites + Payment + Search           |
| **Validation**     | Title, CategoryId, Price required             |

---

## ✨ Summary

✅ **Controllers**: 1 controller (ItemController) với 14 endpoints  
✅ **Services**: ItemService interface + ItemServiceImpl  
✅ **DTOs**: 7 request + 5 response classes  
✅ **Models**: Item entity + 5 related entities  
✅ **Repositories**: ItemRepository + FavoriteItemRepository  
✅ **Enums**: 3 enum types (Status, Condition, TransactionType)  
✅ **Features**: CRUD, Search, Favorites, Payment Integration  
✅ **Authentication**: JWT Bearer Token  
✅ **Database**: PostgreSQL with proper relationships  
✅ **External APIs**: Cloudinary, VNPay, Auth Service

**Toàn bộ API đã được tài liệu hóa chi tiết!** 📚

---

**Generated**: January 2024  
**Service**: Core Service - Items/Posts API  
**Framework**: Spring Boot 3.x  
**Database**: PostgreSQL  
**Status**: ✅ Complete Documentation
