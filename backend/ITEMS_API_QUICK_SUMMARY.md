# Items API - Quick Summary

## 📋 Overview

Đây là tài liệu tóm tắt API quản lý tin đăng (Items/Posts) trong Core Service của secondhand-platform-backend. API này cung cấp các chức năng CRUD đầy đủ, quản lý ảnh, đặc tính sản phẩm, danh sách yêu thích và tích hợp thanh toán VNPay.

---

## 🔗 API Endpoints

### Base URL

```
POST /api/items              ✅ Tạo tin đăng mới
GET /api/items               📖 Lấy tất cả tin
GET /api/items/me            📖 Lấy tin của tôi
GET /api/items/{itemId}      📖 Lấy chi tiết tin
GET /api/items/category/{categoryId}        📖 Lấy tin theo danh mục (ID)
GET /api/items/category/slug/{slug}         📖 Lấy tin theo danh mục (Slug)
GET /api/items/user/{userId}                📖 Lấy tin của user khác
GET /api/items/favorites/me                 📖 Lấy tin yêu thích của tôi
GET /api/items/payment-callback             💳 Callback VNPay

PUT /api/items/{itemId}      ✏️ Cập nhật tin
PATCH /api/items/{itemId}/status           ✏️ Cập nhật status

DELETE /api/items/{itemId}                  🗑️ Xóa tin

POST /api/items/{itemId}/favorite           ❤️ Thêm yêu thích
DELETE /api/items/{itemId}/favorite         ❤️ Xóa yêu thích
```

---

## 📤 Request Format

### Create/Update Item (ItemRequest)

```json
{
  "title": "iPhone 13 Pro Max", // ✅ Bắt buộc
  "description": "Máy mới 100%", // Optional
  "categoryId": "CAT001", // ✅ Bắt buộc
  "price": 25000000, // ✅ Bắt buộc
  "condition": "LIKE_NEW", // NEW/LIKE_NEW/USED/FOR_PARTS
  "transactionType": "SELL", // SELL/GIVE_AWAY
  "status": "ACTIVE", // See Status Enum below
  "location": {
    "address": "123 Nguyễn Huệ",
    "ward": "Bến Thành",
    "district": "Quận 1",
    "city": "TP. Hồ Chí Minh"
  },
  "itemImageList": [
    // Upload ảnh
    { "imageUrl": "https://...", "isPrimary": true }
  ],
  "attributes": [
    // Attributes theo danh mục
    { "code": "brand", "value": "Apple" },
    { "code": "storage", "value": "256GB" }
  ]
}
```

### Update Status (ItemStatusUpdateRequest)

```json
{
  "status": "SOLD" // AVAILABLE/RESERVED/SOLD/HIDDEN/ACTIVE
}
```

---

## 📥 Response Format

### Success (ItemResponse)

```json
{
  "itemId": "ITM00000000000001",
  "title": "iPhone 13 Pro Max",
  "description": "Máy mới 100%",
  "categoryId": "CAT001",
  "price": 25000000,
  "condition": "LIKE_NEW",
  "transactionType": "SELL",
  "status": "ACTIVE",
  "location": {...},
  "userId": "USR00000000000001",
  "createdAt": "2024-01-15T10:30:45",
  "updatedAt": "2024-01-15T10:30:45",
  "itemImageList": [...],
  "attributes": [...],
  "transactionId": "VNP123456",
  "paymentUrl": "https://..."
}
```

### Message Response

```json
{
  "message": "Operation successful",
  "success": true
}
```

---

## 🗄️ Database Schema - Item Entity

### Table: `items`

| Field                  | Type          | Required | Notes                                                     |
| ---------------------- | ------------- | -------- | --------------------------------------------------------- |
| `item_id`              | VARCHAR(20)   | ✅       | ITM + 14 digits                                           |
| `title`                | TEXT          | ✅       | Tiêu đề                                                   |
| `description`          | TEXT          | ❌       | Mô tả                                                     |
| `price`                | DECIMAL(19,2) | ✅       | Giá (VND)                                                 |
| `transaction_type`     | ENUM          | ❌       | SELL, GIVE_AWAY                                           |
| `condition`            | ENUM          | ❌       | NEW, LIKE_NEW, USED, FOR_PARTS                            |
| `status`               | ENUM          | ✅       | AVAILABLE, RESERVED, SOLD, HIDDEN, ACTIVE, DRAFT, EXPIRED |
| `view`                 | INT           | ❌       | Số lượt xem                                               |
| `created_at`           | DATETIME      | ✅       | Tự set                                                    |
| `updated_at`           | DATETIME      | ❌       | Tự update                                                 |
| `user_id`              | VARCHAR(20)   | ✅       | Chủ sở hữu                                                |
| `category_id`          | VARCHAR(20)   | ✅       | FK → Category                                             |
| `transaction_id`       | VARCHAR(50)   | ❌       | VNPay ID                                                  |
| `payment_url`          | TEXT          | ❌       | VNPay URL                                                 |
| `payment_initiated_at` | DATETIME      | ❌       | Payment timeout                                           |

### Related Tables

- `item_images` (1:N) - Ảnh của tin
- `item_attribute_values` (1:N) - Đặc tính của tin
- `locations` (1:1) - Vị trí
- `favorite_items` (1:N) - Danh sách yêu thích
- `reviews` (1:N) - Đánh giá
- `giveaway_requests` (1:N) - Yêu cầu cho tặng
- `reports` (1:N) - Báo cáo
- `notifications` (1:N) - Thông báo

---

## 🎯 Enum Values

### ItemStatus

```
AVAILABLE    → Tin có sẵn (SELL)
RESERVED     → Tin đã giữ
SOLD         → Tin đã bán
HIDDEN       → Ẩn tin
ACTIVE       → Tin hoạt động (GIVE_AWAY)
DRAFT        → Chờ thanh toán (SELL)
EXPIRED      → Tin hết hạn
```

### ItemCondition

```
NEW          → Hàng mới
LIKE_NEW     → Như mới
USED         → Đã sử dụng
FOR_PARTS    → Bộ phận thay thế
```

### TransactionType

```
SELL         → Bán
GIVE_AWAY    → Cho tặng
```

---

## 🔑 Key Features

### 1. Create (Tạo Tin)

- ✅ Hỗ trợ multipart upload ảnh + JSON
- ✅ Hỗ trợ JSON thuần
- ✅ Auto upload ảnh lên Cloudinary
- ✅ Validate dữ liệu đầu vào
- ✅ Tích hợp VNPay cho SELL items
- ✅ Lưu attributes theo danh mục
- ✅ Tự sinh ID: ITM + 14 chữ số

**SELL items**: Status = DRAFT, chờ thanh toán  
**GIVE_AWAY items**: Status = ACTIVE, phát hành ngay

### 2. Read (Đọc Tin)

- ✅ Lấy tất cả / tin của tôi / theo ID
- ✅ Lọc theo danh mục (ID/Slug)
- ✅ Lấy tin của user khác
- ✅ Lấy danh sách yêu thích

### 3. Update (Cập Nhật)

- ✅ Cập nhật toàn bộ thông tin
- ✅ Cập nhật riêng status
- ✅ Cập nhật location, images, attributes
- ✅ Chỉ chủ sở hữu có quyền
- ✅ Auto update timestamp

### 4. Delete (Xóa)

- ✅ Xóa tin hoàn toàn
- ✅ Cascade delete (images, attributes, etc.)
- ✅ Chỉ chủ sở hữu có quyền

### 5. Favorite (Yêu Thích)

- ✅ Thêm/xóa yêu thích
- ✅ Lấy danh sách yêu thích
- ✅ Kiểm tra trùng lặp tự động

### 6. Payment (Thanh Toán)

- ✅ Tạo VNPay payment URL
- ✅ Xử lý VNPay callback
- ✅ Xác thực secure hash
- ✅ Auto update status = ACTIVE khi thành công

---

## 🛡️ Validation Rules

### ItemRequest

```
✅ Required:
  - title (not blank)
  - categoryId (must exist)
  - price (not null)

📋 Conditional:
  - SELL: price > 0
  - GIVE_AWAY: price >= 0

📝 Optional:
  - condition: NEW, LIKE_NEW, USED, FOR_PARTS
  - transactionType: SELL, GIVE_AWAY
  - status: valid enum
  - images: JPG, PNG, GIF, WEBP, max 10MB
```

### Images

```
✅ Formats: JPEG, PNG, GIF, WebP
✅ Max size: 10MB per file
✅ Upload: Cloudinary
✅ Stored as: itemImageList
```

---

## 🔐 Authentication & Authorization

### Auth Requirements

```
Authentication: JWT Bearer token (required for write/delete operations)
Authorization:
  - Create: Any logged-in user
  - Read: Public (no auth required)
  - Update: Item owner only
  - Delete: Item owner only
  - Update Status: Item owner only
  - Add/Remove Favorite: Any logged-in user
```

### Error Responses

- **401 Unauthorized**: No/invalid JWT token
- **403 Forbidden**: Not item owner
- **400 Bad Request**: Validation error
- **404 Not Found**: Item doesn't exist

---

## 💳 Payment Flow (SELL Items)

```
1. User tạo tin (POST /api/items)
   │
   ├─ ItemStatus = DRAFT
   ├─ paymentUrl = VNPay URL
   ├─ transactionId = generated
   │
2. Return paymentUrl to user
   │
3. User truy cập paymentUrl → VNPay gateway
   │
4. User thanh toán
   │
5. VNPay redirect → GET /payment-callback
   │
6. System xử lý callback
   │
   ├─ If responseCode == "00":
   │  └─ ItemStatus = DRAFT → ACTIVE
   │  └─ Redirect: /payment-success
   │
   └─ If failed:
      └─ Item stays DRAFT or deleted
      └─ Redirect: /payment-failed

Timeout: 15 minutes
```

---

## 📂 File Structure

```
core-service/
├── controller/
│   └── ItemController.java
├── service/
│   ├── ItemService.java (interface)
│   └── impl/
│       └── ItemServiceImpl.java
├── dto/
│   ├── request/
│   │   ├── ItemRequest.java
│   │   ├── ItemStatusUpdateRequest.java
│   │   ├── ItemImageRequest.java
│   │   ├── ItemAttributeRequest.java
│   │   ├── LocationRequest.java
│   │   └── VNPayCallbackRequest.java
│   └── response/
│       ├── ItemResponse.java
│       ├── ItemImageResponse.java
│       ├── LocationResponse.java
│       ├── ItemAttributeResponse.java
│       └── MessageResponse.java
├── model/
│   ├── Item.java
│   ├── ItemImage.java
│   ├── ItemAttributeValue.java
│   ├── Location.java
│   ├── FavoriteItem.java
│   └── enums/
│       ├── ItemStatus.java
│       ├── ItemCondition.java
│       └── TransactionType.java
├── repository/
│   ├── ItemRepository.java
│   ├── FavoriteItemRepository.java
│   ├── ItemImageRepository.java
│   └── (other repositories)
```

---

## 📊 Database Queries (ItemRepository)

```java
// Find by ID
Optional<Item> findByItemId(String itemId);

// Find all by user
List<Item> findByUserId(String userId);

// Find by category
List<Item> findByCategory_CategoryId(String categoryId);
List<Item> findByCategory_SlugAndStatusIn(String slug, List<ItemStatus> statuses);

// Find all (default JPA)
List<Item> findAll();
```

---

## 🔗 Related Services

### CloudinaryService

```
uploadImage(MultipartFile file) → String (image URL)
Supported: JPG, PNG, GIF, WebP
Max size: 10MB
```

### PaymentEventService

```
createVnPayPayment(amount, bankCode, language, userId)
→ CreatePaymentResponse {
    code: "00" (success),
    message: String,
    transactionId: String,
    paymentUrl: String
  }
```

### UserServiceClient

```
Verify user exists
Get user details via grpc/feign
```

---

## 📝 Example Requests

### Create Item

```bash
curl -X POST "http://localhost:8080/api/items" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -F "item={\"title\":\"iPhone 13\",\"categoryId\":\"CAT001\",\"price\":25000000};type=application/json" \
  -F "images=@image1.jpg" \
  -F "images=@image2.jpg"
```

### Get All Items

```bash
curl -X GET "http://localhost:8080/api/items"
```

### Get My Items

```bash
curl -X GET "http://localhost:8080/api/items/me" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Update Item

```bash
curl -X PUT "http://localhost:8080/api/items/ITM00000000000001" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"New Title","price":20000000}'
```

### Delete Item

```bash
curl -X DELETE "http://localhost:8080/api/items/ITM00000000000001" \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Add Favorite

```bash
curl -X POST "http://localhost:8080/api/items/ITM00000000000001/favorite" \
  -H "Authorization: Bearer JWT_TOKEN"
```

---

## ⚠️ Common Errors

### 400 Bad Request

```json
{
  "message": "Price must be greater than 0 for SELL items"
}
```

### 404 Not Found

```json
{
  "message": "Item not found with id: ITM00000000000999"
}
```

### 401 Unauthorized

```json
{
  "message": "Invalid or missing JWT token"
}
```

### 403 Forbidden

```json
{
  "message": "You do not have permission to update this item"
}
```

---

## 📌 Best Practices

1. **Always use JWT token** for write operations
2. **Validate images** before upload (type + size)
3. **Handle payment timeout** (15 min for SELL items)
4. **Check authorization** before update/delete
5. **Use pagination** for large lists (not yet implemented)
6. **Cache category data** for faster filtering
7. **Index userId, categoryId, status** in database

---

## 🚀 Performance Tips

- Lazy load relationships (Category, Location)
- Join fetch where needed
- Add database indexes on frequently queried fields
- Implement pagination for list endpoints
- Cache static data (categories, attributes)
- Batch image uploads to Cloudinary
- Use CDN for image serving

---

## 📚 Documentation Files

- **ITEMS_API_DOCUMENTATION.md** - Complete API reference
- **ITEMS_API_IMPLEMENTATION.md** - Implementation details, service logic
- **ITEMS_API_EXAMPLES.md** - CURL examples & response formats
- **ITEMS_API_QUICK_SUMMARY.md** - This file (quick reference)

---

## 🔗 Related Files in Project

```
backend/
├── core-service/src/main/java/com/secondhand/coreservice/
│   ├── controller/ItemController.java
│   ├── service/ItemService.java
│   ├── service/impl/ItemServiceImpl.java
│   ├── dto/request/ItemRequest.java
│   ├── dto/response/ItemResponse.java
│   ├── model/Item.java
│   └── repository/ItemRepository.java
│
└── (See full structure in ITEMS_API_DOCUMENTATION.md)
```

---

## 📞 Support

For issues or questions:

1. Check ITEMS_API_DOCUMENTATION.md for detailed API reference
2. Check ITEMS_API_EXAMPLES.md for CURL examples
3. Check ITEMS_API_IMPLEMENTATION.md for service logic
4. Review Item.java model for entity structure
5. Check ItemRepository for custom queries

---

**Generated**: January 2024  
**Service**: Core Service  
**Framework**: Spring Boot 3.x  
**Database**: PostgreSQL  
**Status**: ✅ Complete & Documented
