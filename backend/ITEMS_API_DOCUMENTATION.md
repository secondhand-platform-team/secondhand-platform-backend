# Items/Posts API Documentation - Core Service

## 📋 Tổng Quan

Đây là tài liệu chi tiết về API quản lý tin đăng (Items/Posts) trong Core Service. Các phương thức thực hiện CRUD operations, quản lý yêu thích, và tích hợp thanh toán VNPay.

---

## 🎯 Đường dẫn API Base

```
Base URL: /api/items
```

---

## 📌 Danh Sách Các Endpoints

### 1. CREATE - Tạo Tin Đăng Mới

#### 1.1 Tạo với Upload Ảnh (Multipart)

```http
POST /api/items
Content-Type: multipart/form-data

Request Parts:
  - item (JSON string): Thông tin tin đăng
  - images (MultipartFile[]): Mảng ảnh (optional)

Response: 201 Created
```

#### 1.2 Tạo bằng JSON (không ảnh)

```http
POST /api/items/json
Content-Type: application/json

Response: 201 Created
```

---

### 2. READ - Lấy Thông Tin Tin Đăng

#### 2.1 Lấy Tất Cả Tin Đăng

```http
GET /api/items

Query Parameters: Không có
Response: 200 OK
Body: List<ItemResponse>
```

#### 2.2 Lấy Tin Đăng Của Người Dùng Hiện Tại

```http
GET /api/items/me

Authentication: Required (JWT Token)
Response: 200 OK
Body: List<ItemResponse>
```

#### 2.3 Lấy Chi Tiết Tin Đăng Theo ID

```http
GET /api/items/{itemId}

Path Parameters:
  - itemId (String): ID của tin đăng (bắt đầu từ "ITM")

Response: 200 OK
Body: ItemResponse
```

#### 2.4 Lấy Tin Đăng Theo Danh Mục (ID)

```http
GET /api/items/category/{categoryId}

Path Parameters:
  - categoryId (String): ID của danh mục

Response: 200 OK
Body: List<ItemResponse>
```

#### 2.5 Lấy Tin Đăng Theo Danh Mục (Slug)

```http
GET /api/items/category/slug/{slug}

Path Parameters:
  - slug (String): Slug của danh mục (VD: "dien-thoai")

Response: 200 OK
Body: List<ItemResponse> - Chỉ trả ACTIVE hoặc AVAILABLE items
```

#### 2.6 Lấy Tin Đăng Của Người Dùng Cụ Thể

```http
GET /api/items/user/{userId}

Path Parameters:
  - userId (String): ID của người dùng

Response: 200 OK
Body: List<ItemResponse>
```

#### 2.7 Lấy Danh Sách Tin Yêu Thích Của Tôi

```http
GET /api/items/favorites/me

Authentication: Required (JWT Token)
Response: 200 OK
Body: List<ItemResponse>
```

---

### 3. UPDATE - Cập Nhật Tin Đăng

#### 3.1 Cập Nhật Toàn Bộ Thông Tin

```http
PUT /api/items/{itemId}
Content-Type: application/json

Path Parameters:
  - itemId (String): ID của tin đăng

Authentication: Required (JWT Token)
Authorization: Phải là chủ sở hữu của tin đăng

Response: 200 OK
Body: ItemResponse
```

#### 3.2 Cập Nhật Chỉ Trạng Thái (Status)

```http
PATCH /api/items/{itemId}/status
Content-Type: application/json

Path Parameters:
  - itemId (String): ID của tin đăng

Authentication: Required (JWT Token)
Authorization: Phải là chủ sở hữu của tin đăng

Response: 200 OK
Body: ItemResponse
```

---

### 4. DELETE - Xóa Tin Đăng

```http
DELETE /api/items/{itemId}

Path Parameters:
  - itemId (String): ID của tin đăng

Authentication: Required (JWT Token)
Authorization: Phải là chủ sở hữu của tin đăng

Response: 200 OK
Body: MessageResponse
```

---

### 5. FAVORITE - Quản Lý Tin Yêu Thích

#### 5.1 Thêm Tin Vào Danh Sách Yêu Thích

```http
POST /api/items/{itemId}/favorite

Path Parameters:
  - itemId (String): ID của tin đăng

Authentication: Required (JWT Token)
Response: 200 OK
Body: MessageResponse
```

#### 5.2 Xóa Tin Khỏi Danh Sách Yêu Thích

```http
DELETE /api/items/{itemId}/favorite

Path Parameters:
  - itemId (String): ID của tin đăng

Authentication: Required (JWT Token)
Response: 200 OK
Body: MessageResponse
```

---

### 6. PAYMENT - Callback Thanh Toán

```http
GET /api/items/payment-callback

Query Parameters (VNPay):
  - vnp_Amount: Số tiền thanh toán
  - vnp_BankCode: Mã ngân hàng
  - vnp_BankTranNo: Số hiệu giao dịch ngân hàng
  - vnp_CardType: Loại thẻ
  - vnp_OrderInfo: Thông tin đơn hàng
  - vnp_PayDate: Ngày thanh toán (YYYYMMDDHHmmss)
  - vnp_ResponseCode: Mã phản hồi (00 = thành công)
  - vnp_TmnCode: Mã cửa hàng
  - vnp_TransactionNo: Mã giao dịch VNPay
  - vnp_TransactionStatus: Trạng thái giao dịch
  - vnp_TxnRef: Mã tham chiếu
  - vnp_SecureHash: Mã xác minh bảo mật

Response: 302 Redirect
  - Success: http://localhost:3000/payment-success?status=success&transactionId={transactionId}
  - Error: http://localhost:3000/payment-failed?status=error&message={message}
```

---

## 📤 Request Formats

### ItemRequest (Tạo/Cập Nhật Tin Đăng)

```json
{
  "title": "iPhone 13 Pro Max",
  "description": "Máy mới 100%, full phụ kiện",
  "categoryId": "CAT001",
  "price": 25000000,
  "condition": "LIKE_NEW",
  "transactionType": "SELL",
  "status": "ACTIVE",
  "location": {
    "address": "123 Nguyễn Hue",
    "ward": "Ben Thanh",
    "district": "District 1",
    "city": "Ho Chi Minh City"
  },
  "itemImageList": [
    {
      "imageUrl": "https://res.cloudinary.com/...",
      "isPrimary": true
    },
    {
      "imageUrl": "https://res.cloudinary.com/...",
      "isPrimary": false
    }
  ],
  "attributes": [
    {
      "code": "brand",
      "value": "Apple"
    },
    {
      "code": "storage",
      "value": "256GB"
    },
    {
      "code": "color",
      "value": "Black"
    }
  ],
  "transactionId": "VNP123456",
  "orderId": "ORD123456",
  "responseCode": "00",
  "secureHash": "..."
}
```

**Chú Thích:**

- `title`: Bắt buộc (required)
- `categoryId`: Bắt buộc (required)
- `price`: Bắt buộc (required), phải > 0 cho SELL, có thể = 0 cho GIVE_AWAY
- `condition`: Optional - NEW, LIKE_NEW, USED, FOR_PARTS
- `transactionType`: SELL hoặc GIVE_AWAY (default: SELL)
- `status`: ACTIVE, DRAFT, AVAILABLE, RESERVED, SOLD, HIDDEN, EXPIRED
- `attributes`: Phụ thuộc vào danh mục (category)

---

### ItemStatusUpdateRequest

```json
{
  "status": "ACTIVE"
}
```

**Giá Trị Hợp Lệ:**

- `AVAILABLE` - Tin đang có sẵn
- `RESERVED` - Tin đã được giữ chỗ
- `SOLD` - Tin đã bán
- `HIDDEN` - Ẩn tin
- `ACTIVE` - Tin hoạt động (cho GIVE_AWAY)
- `DRAFT` - Nháp (chờ thanh toán cho SELL)
- `EXPIRED` - Tin hết hạn

---

## 📥 Response Formats

### ItemResponse (Thành Công)

```json
{
  "itemId": "ITM00000001",
  "title": "iPhone 13 Pro Max",
  "description": "Máy mới 100%, full phụ kiện",
  "categoryId": "CAT001",
  "price": 25000000,
  "condition": "LIKE_NEW",
  "transactionType": "SELL",
  "status": "ACTIVE",
  "location": {
    "address": "123 Nguyễn Hue",
    "ward": "Ben Thanh",
    "district": "District 1",
    "city": "Ho Chi Minh City"
  },
  "userId": "USR00000001",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "itemImageList": [
    {
      "imageUrl": "https://res.cloudinary.com/...",
      "isPrimary": true
    },
    {
      "imageUrl": "https://res.cloudinary.com/...",
      "isPrimary": false
    }
  ],
  "attributes": [
    {
      "attributeId": "ATTR001",
      "code": "brand",
      "name": "Hãng sản xuất",
      "description": "Hãng sản xuất của điện thoại",
      "dataType": "STRING",
      "unit": null,
      "value": "Apple"
    },
    {
      "attributeId": "ATTR002",
      "code": "storage",
      "name": "Dung lượng lưu trữ",
      "description": "Dung lượng bộ nhớ",
      "dataType": "STRING",
      "unit": "GB",
      "value": "256GB"
    }
  ],
  "transactionId": "VNP123456",
  "paymentUrl": "https://sandbox.vnpayment.vn/paygate/pay?..."
}
```

### MessageResponse (Thành Công)

```json
{
  "message": "Item deleted successfully",
  "success": true
}
```

### LocationResponse

```json
{
  "address": "123 Nguyễn Hue",
  "ward": "Ben Thanh",
  "district": "District 1",
  "city": "Ho Chi Minh City"
}
```

### ItemImageResponse

```json
{
  "imageUrl": "https://res.cloudinary.com/...",
  "isPrimary": true
}
```

### ItemAttributeResponse

```json
{
  "attributeId": "ATTR001",
  "code": "brand",
  "name": "Hãng sản xuất",
  "description": "Hãng sản xuất của điện thoại",
  "dataType": "STRING",
  "unit": null,
  "value": "Apple"
}
```

---

## 🗄️ Database Schema - Item Entity

### Bảng: `items`

| Cột                    | Kiểu Dữ Liệu   | Bắt Buộc       | Mô Tả                                                     |
| ---------------------- | -------------- | -------------- | --------------------------------------------------------- |
| `item_id`              | VARCHAR(20)    | ✅ PRIMARY KEY | ID tin đăng (tự sinh: ITM+14 số)                          |
| `title`                | TEXT           | ✅             | Tiêu đề tin đăng                                          |
| `description`          | TEXT           | ❌             | Mô tả chi tiết                                            |
| `price`                | DECIMAL(19, 2) | ✅             | Giá bán/cho (VND)                                         |
| `transaction_type`     | ENUM           | ❌             | SELL hoặc GIVE_AWAY                                       |
| `condition`            | ENUM           | ❌             | NEW, LIKE_NEW, USED, FOR_PARTS                            |
| `status`               | ENUM           | ✅             | AVAILABLE, RESERVED, SOLD, HIDDEN, ACTIVE, DRAFT, EXPIRED |
| `view`                 | INT            | ❌             | Số lượt xem                                               |
| `location`             | TEXT           | ❌             | Vị trí (deprecated - dùng bảng Location)                  |
| `created_at`           | DATETIME       | ✅             | Thời gian tạo                                             |
| `updated_at`           | DATETIME       | ❌             | Thời gian cập nhật cuối                                   |
| `user_id`              | VARCHAR(20)    | ✅             | ID chủ sở hữu                                             |
| `transaction_id`       | VARCHAR(50)    | ❌             | ID giao dịch VNPay                                        |
| `payment_url`          | TEXT           | ❌             | URL thanh toán VNPay                                      |
| `payment_initiated_at` | DATETIME       | ❌             | Thời gian bắt đầu thanh toán (hết hạn sau 15 phút)        |
| `category_id`          | VARCHAR(20)    | ✅ FOREIGN KEY | Tham chiếu Category                                       |

### Enum Types

#### ItemStatus

```
- AVAILABLE: Tin có sẵn (SELL)
- RESERVED: Tin đã được giữ
- SOLD: Tin đã bán
- HIDDEN: Ẩn tin
- ACTIVE: Tin hoạt động (GIVE_AWAY)
- DRAFT: Nháp chờ thanh toán
- EXPIRED: Tin hết hạn
```

#### ItemCondition

```
- NEW: Mới
- LIKE_NEW: Như mới
- USED: Đã sử dụng
- FOR_PARTS: Bộ phận thay thế
```

#### TransactionType

```
- SELL: Bán
- GIVE_AWAY: Cho tặng
```

---

## 📊 Mối Quan Hệ (Relationships)

```
Item (1) ──── (N) ItemImage
Item (1) ──── (N) ItemAttributeValue
Item (1) ──── (1) Location
Item (N) ──── (1) Category
Item (1) ──── (N) FavoriteItem
Item (1) ──── (N) Review
Item (1) ──── (N) GiveawayRequest
Item (1) ──── (N) Report
Item (1) ──── (N) Notification
```

---

## 🔑 Các Chức Năng Chính

### 1. Create (Tạo Tin)

- ✅ Hỗ trợ tạo từ multipart (ảnh + JSON) hoặc JSON thuần
- ✅ Tự động upload ảnh lên Cloudinary
- ✅ Xác thực dữ liệu (validation)
- ✅ Tích hợp thanh toán VNPay cho SELL items
- ✅ Tạo Location nếu có
- ✅ Lưu attributes dựa trên category

### 2. Read (Đọc Tin)

- ✅ Lấy tất cả tin (getAllItems)
- ✅ Lấy tin của user hiện tại (getMyItems)
- ✅ Lấy tin theo ID (getItemById)
- ✅ Lấy tin theo danh mục (getItemsByCategory, getItemsByCategorySlug)
- ✅ Lấy tin của user khác (getItemsByUser)
- ✅ Lấy danh sách yêu thích (getMyFavoriteItems)

### 3. Update (Cập Nhật)

- ✅ Cập nhật toàn bộ thông tin: title, description, price, condition, etc.
- ✅ Cập nhật location
- ✅ Cập nhật images (xóa cũ, thêm mới)
- ✅ Cập nhật attributes
- ✅ Chỉ chủ sở hữu có quyền update
- ✅ Cập nhật timestamp updatedAt tự động

### 4. Update Status (Cập Nhật Trạng Thái)

- ✅ Riêng endpoint PATCH để cập nhật status
- ✅ Xác thực enum values
- ✅ Chỉ chủ sở hữu có quyền thay đổi

### 5. Delete (Xóa)

- ✅ Xóa tin hoàn toàn
- ✅ Xóa cascade: images, attributes, locations, favorites
- ✅ Chỉ chủ sở hữu có quyền xóa

### 6. Favorite (Yêu Thích)

- ✅ Thêm tin vào danh sách yêu thích
- ✅ Xóa tin khỏi danh sách yêu thích
- ✅ Kiểm tra trùng lặp
- ✅ Lấy danh sách tin yêu thích của user

### 7. Payment (Thanh Toán)

- ✅ Tạo payment URL cho SELL items
- ✅ Xử lý VNPay callback
- ✅ Xác thực secure hash
- ✅ Cập nhật trạng thái tin sau thanh toán

---

## 🛡️ Validation Rules

### ItemRequest Validation:

```
- title: @NotBlank - Bắt buộc, không được trống
- categoryId: @NotBlank - Bắt buộc, phải tồn tại
- price: @NotNull - Bắt buộc
  * SELL: price > 0
  * GIVE_AWAY: price >= 0
- condition: Enum - NEW, LIKE_NEW, USED, FOR_PARTS
- transactionType: Enum - SELL, GIVE_AWAY
- status: Enum - AVAILABLE, RESERVED, SOLD, HIDDEN, ACTIVE, DRAFT, EXPIRED
- images: .jpg, .jpeg, .png, .gif, .webp, max 10MB mỗi file
```

### ItemStatusUpdateRequest:

```
- status: @NotBlank - Bắt buộc
           Enum - AVAILABLE, RESERVED, SOLD, HIDDEN, ACTIVE
```

---

## 🔐 Authentication & Authorization

- **Authentication**: JWT Token (Bearer)
- **Authorization**:
  - Create: Chí có user đã đăng nhập
  - Update: Chỉ chủ sở hữu
  - Delete: Chỉ chủ sở hữu
  - Update Status: Chỉ chủ sở hữu
  - Add/Remove Favorite: Chỉ user đã đăng nhập
  - Get My Items: Chỉ user đó
  - Get My Favorites: Chỉ user đó

---

## 💾 Database Queries

### ItemRepository Methods:

```java
// Lấy tin theo ID
Optional<Item> findByItemId(String itemId);

// Lấy tất cả tin của user
List<Item> findByUserId(String userId);

// Lấy tin theo danh mục
List<Item> findByCategory_CategoryId(String categoryId);
List<Item> findByCategory_SlugAndStatus(String slug, ItemStatus status);
List<Item> findByCategory_SlugAndStatusIn(String slug, List<ItemStatus> statuses);
```

### FavoriteItemRepository Methods:

```java
// Kiểm tra tin đã yêu thích
boolean existsByUserIdAndItem_ItemId(String userId, String itemId);

// Lấy tin yêu thích của user
List<FavoriteItem> findByUserId(String userId);

// Lấy chi tiết tin yêu thích
Optional<FavoriteItem> findByUserIdAndItem_ItemId(String userId, String itemId);
```

---

## 🔄 Luồng Thanh Toán (Payment Flow)

### Cho SELL Items:

```
1. User tạo tin (POST /api/items)
2. System tạo payment URL (VNPay)
3. Tin được lưu ở status DRAFT
4. transactionId & paymentUrl được trả về
5. User truy cập paymentUrl để thanh toán
6. VNPay redirect về callback URL
7. System xử lý callback
8. Nếu thành công (responseCode="00"):
   - Cập nhật status = ACTIVE
   - Redirect về payment-success page
9. Nếu thất bại:
   - Tin vẫn ở DRAFT hoặc bị xóa
   - Redirect về payment-failed page
```

### Cho GIVE_AWAY Items:

```
1. User tạo tin (POST /api/items)
2. Không cần thanh toán
3. Tin được lưu ở status ACTIVE ngay lập tức
4. paymentUrl = null
```

---

## 📝 Mapping Logic

### Attribute Data Types:

```
STRING → valueString
NUMBER → valueNumber (BigDecimal)
INTEGER → valueInteger (Long)
BOOLEAN → valueBoolean
DATE → valueDate (LocalDate)
ENUM → valueString
JSON → valueJson (JSON string)
```

---

## 🎨 File Structure

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
│   └── FavoriteItemRepository.java
```

---

## ⚠️ Exception Handling

### Custom Exceptions:

```
- BadRequestException: Validation lỗi, dữ liệu không hợp lệ
- ResourceNotFoundException: Không tìm thấy item/category/user
- UnauthorizedException: Không có quyền truy cập
```

---

## 📌 Notes & Best Practices

1. **Image Upload**: Tối đa 10MB/file, hỗ trợ JPG, PNG, GIF, WEBP
2. **Payment Timeout**: VNPAY payment hết hạn sau 15 phút
3. **ID Generation**: ItemId tự sinh với pattern "ITM" + 14 số
4. **Timestamp**: createdAt immutable, updatedAt tự cập nhật
5. **Authorization**: Luôn kiểm tra userId từ JWT token
6. **Cascade Delete**: Xóa item sẽ xóa tất cả related records
7. **Pagination**: Hiện tại chưa có pagination, trả về toàn bộ list

---

**Last Updated**: January 2024
**Service**: Core Service
**Framework**: Spring Boot 3.x
**Database**: PostgreSQL
