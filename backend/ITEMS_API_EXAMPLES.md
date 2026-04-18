# Items API - CURL & Postman Examples

## 🔗 Base URL

```
http://localhost:8080/api/items
```

## 🔐 Authentication Headers

```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

---

## 📝 1. CREATE ENDPOINTS

### 1.1 Tạo Tin Đăng với Upload Ảnh (Multipart)

#### CURL

```bash
# Tạo tin đăng kiểu bán (SELL)
curl -X POST "http://localhost:8080/api/items" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "item={
    \"title\": \"iPhone 13 Pro Max\",
    \"description\": \"Máy mới 100%, full phụ kiện\",
    \"categoryId\": \"CAT001\",
    \"price\": 25000000,
    \"condition\": \"LIKE_NEW\",
    \"transactionType\": \"SELL\",
    \"location\": {
      \"address\": \"123 Nguyễn Huệ\",
      \"ward\": \"Bến Thành\",
      \"district\": \"Quận 1\",
      \"city\": \"TP. Hồ Chí Minh\"
    },
    \"attributes\": [
      {\"code\": \"brand\", \"value\": \"Apple\"},
      {\"code\": \"storage\", \"value\": \"256GB\"},
      {\"code\": \"color\", \"value\": \"Black\"}
    ]
  };type=application/json" \
  -F "images=@/path/to/image1.jpg" \
  -F "images=@/path/to/image2.jpg" \
  -F "images=@/path/to/image3.jpg"

# Tạo tin cho tặng (GIVE_AWAY)
curl -X POST "http://localhost:8080/api/items" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "item={
    \"title\": \"Tặng sách tiếng Anh cũ\",
    \"description\": \"Sách tiếng Anh, còn rất tốt\",
    \"categoryId\": \"CAT002\",
    \"price\": 0,
    \"condition\": \"USED\",
    \"transactionType\": \"GIVE_AWAY\",
    \"attributes\": [
      {\"code\": \"language\", \"value\": \"English\"}
    ]
  };type=application/json" \
  -F "images=@/path/to/book.jpg"
```

#### Response Success (201 Created)

```json
{
  "itemId": "ITM00000000000001",
  "title": "iPhone 13 Pro Max",
  "description": "Máy mới 100%, full phụ kiện",
  "categoryId": "CAT001",
  "price": 25000000,
  "condition": "LIKE_NEW",
  "transactionType": "SELL",
  "status": "DRAFT",
  "location": {
    "address": "123 Nguyễn Huệ",
    "ward": "Bến Thành",
    "district": "Quận 1",
    "city": "TP. Hồ Chí Minh"
  },
  "userId": "USR00000000000001",
  "createdAt": "2024-01-15T10:30:45",
  "updatedAt": "2024-01-15T10:30:45",
  "itemImageList": [
    {
      "imageUrl": "https://res.cloudinary.com/secondhand/image/upload/v1705321845/image1_abc123.jpg",
      "isPrimary": true
    },
    {
      "imageUrl": "https://res.cloudinary.com/secondhand/image/upload/v1705321846/image2_def456.jpg",
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
  "transactionId": "VNP20240115103045ABC",
  "paymentUrl": "https://sandbox.vnpayment.vn/paygate/pay?vnp_Amount=2500000000&vnp_BankCode=NCB&..."
}
```

#### Response Failure (400 Bad Request)

```json
{
  "timestamp": "2024-01-15T10:30:45",
  "status": 400,
  "error": "Bad Request",
  "message": "Price must be greater than 0 for SELL items",
  "path": "/api/items"
}
```

---

### 1.2 Tạo Tin Đăng bằng JSON (Không Upload Ảnh)

#### CURL

```bash
curl -X POST "http://localhost:8080/api/items/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Xe máy Honda Future",
    "description": "Xe máy Honda Future 2023, máy mới, full phụ kiện",
    "categoryId": "CAT003",
    "price": 15000000,
    "condition": "NEW",
    "transactionType": "SELL",
    "location": {
      "address": "456 Lê Lợi",
      "ward": "Bến Thành",
      "district": "Quận 1",
      "city": "TP. Hồ Chí Minh"
    },
    "attributes": [
      {
        "code": "brand",
        "value": "Honda"
      },
      {
        "code": "model",
        "value": "Future"
      },
      {
        "code": "year",
        "value": "2023"
      }
    ]
  }'
```

#### Response

```json
{
  "itemId": "ITM00000000000002",
  "title": "Xe máy Honda Future",
  "description": "Xe máy Honda Future 2023, máy mới, full phụ kiện",
  "categoryId": "CAT003",
  "price": 15000000,
  "condition": "NEW",
  "transactionType": "SELL",
  "status": "DRAFT",
  "location": {
    "address": "456 Lê Lợi",
    "ward": "Bến Thành",
    "district": "Quận 1",
    "city": "TP. Hồ Chí Minh"
  },
  "userId": "USR00000000000001",
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00",
  "itemImageList": [],
  "attributes": [
    {
      "code": "brand",
      "value": "Honda"
    },
    {
      "code": "model",
      "value": "Future"
    },
    {
      "code": "year",
      "value": "2023"
    }
  ],
  "transactionId": "VNP20240115110000DEF",
  "paymentUrl": "https://sandbox.vnpayment.vn/paygate/pay?..."
}
```

---

## 📖 2. READ ENDPOINTS

### 2.1 Lấy Tất Cả Tin Đăng

#### CURL

```bash
curl -X GET "http://localhost:8080/api/items"
```

#### Response

```json
[
  {
    "itemId": "ITM00000000000001",
    "title": "iPhone 13 Pro Max",
    "description": "Máy mới 100%, full phụ kiện",
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
    "attributes": [...]
  },
  {
    "itemId": "ITM00000000000002",
    "title": "Xe máy Honda Future",
    ...
  }
]
```

---

### 2.2 Lấy Tin Đăng Của Tôi

#### CURL

```bash
curl -X GET "http://localhost:8080/api/items/me" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Response

```json
[
  {
    "itemId": "ITM00000000000001",
    "title": "iPhone 13 Pro Max",
    ...
  }
]
```

---

### 2.3 Lấy Chi Tiết Tin Đăng Theo ID

#### CURL

```bash
curl -X GET "http://localhost:8080/api/items/ITM00000000000001"
```

#### Response

```json
{
  "itemId": "ITM00000000000001",
  "title": "iPhone 13 Pro Max",
  "description": "Máy mới 100%, full phụ kiện",
  "categoryId": "CAT001",
  "price": 25000000,
  "condition": "LIKE_NEW",
  "transactionType": "SELL",
  "status": "ACTIVE",
  "location": {
    "address": "123 Nguyễn Huệ",
    "ward": "Bến Thành",
    "district": "Quận 1",
    "city": "TP. Hồ Chí Minh"
  },
  "userId": "USR00000000000001",
  "createdAt": "2024-01-15T10:30:45",
  "updatedAt": "2024-01-15T10:30:45",
  "itemImageList": [
    {
      "imageUrl": "https://res.cloudinary.com/...",
      "isPrimary": true
    }
  ],
  "attributes": [
    {
      "code": "brand",
      "value": "Apple"
    }
  ]
}
```

---

### 2.4 Lấy Tin Đăng Theo Danh Mục (ID)

#### CURL

```bash
curl -X GET "http://localhost:8080/api/items/category/CAT001"
```

#### Response

```json
[
  {
    "itemId": "ITM00000000000001",
    "title": "iPhone 13 Pro Max",
    ...
  },
  {
    "itemId": "ITM00000000000005",
    "title": "Samsung Galaxy S23",
    ...
  }
]
```

---

### 2.5 Lấy Tin Đăng Theo Danh Mục (Slug)

#### CURL

```bash
curl -X GET "http://localhost:8080/api/items/category/slug/dien-thoai"
```

#### Note

Chỉ trả về items có status = ACTIVE hoặc AVAILABLE

#### Response

```json
[
  {
    "itemId": "ITM00000000000001",
    "title": "iPhone 13 Pro Max",
    "status": "ACTIVE",
    ...
  }
]
```

---

### 2.6 Lấy Tin Đăng Của User Cụ Thể

#### CURL

```bash
curl -X GET "http://localhost:8080/api/items/user/USR00000000000001"
```

#### Response

```json
[
  {
    "itemId": "ITM00000000000001",
    "title": "iPhone 13 Pro Max",
    "userId": "USR00000000000001",
    ...
  },
  {
    "itemId": "ITM00000000000002",
    "title": "Xe máy Honda Future",
    "userId": "USR00000000000001",
    ...
  }
]
```

---

### 2.7 Lấy Danh Sách Tin Yêu Thích

#### CURL

```bash
curl -X GET "http://localhost:8080/api/items/favorites/me" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Response

```json
[
  {
    "itemId": "ITM00000000000001",
    "title": "iPhone 13 Pro Max",
    ...
  },
  {
    "itemId": "ITM00000000000003",
    "title": "Laptop Dell XPS",
    ...
  }
]
```

---

## ✏️ 3. UPDATE ENDPOINTS

### 3.1 Cập Nhật Toàn Bộ Tin Đăng

#### CURL

```bash
curl -X PUT "http://localhost:8080/api/items/ITM00000000000001" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "iPhone 13 Pro Max - Giá Cực Rẻ",
    "description": "Máy mới 100%, full phụ kiện, còn bảo hành",
    "categoryId": "CAT001",
    "price": 22000000,
    "condition": "LIKE_NEW",
    "transactionType": "SELL",
    "status": "ACTIVE",
    "location": {
      "address": "123 Nguyễn Huệ - Tầng 2",
      "ward": "Bến Thành",
      "district": "Quận 1",
      "city": "TP. Hồ Chí Minh"
    },
    "attributes": [
      {
        "code": "brand",
        "value": "Apple"
      },
      {
        "code": "storage",
        "value": "512GB"
      }
    ]
  }'
```

#### Response

```json
{
  "itemId": "ITM00000000000001",
  "title": "iPhone 13 Pro Max - Giá Cực Rẻ",
  "description": "Máy mới 100%, full phụ kiện, còn bảo hành",
  "categoryId": "CAT001",
  "price": 22000000,
  "condition": "LIKE_NEW",
  "transactionType": "SELL",
  "status": "ACTIVE",
  "location": {
    "address": "123 Nguyễn Huệ - Tầng 2",
    "ward": "Bến Thành",
    "district": "Quận 1",
    "city": "TP. Hồ Chí Minh"
  },
  "userId": "USR00000000000001",
  "createdAt": "2024-01-15T10:30:45",
  "updatedAt": "2024-01-15T14:30:45",
  "itemImageList": [...],
  "attributes": [...]
}
```

---

### 3.2 Cập Nhật Chỉ Trạng Thái (Status)

#### CURL

```bash
# Thay đổi status thành AVAILABLE
curl -X PATCH "http://localhost:8080/api/items/ITM00000000000001/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "AVAILABLE"
  }'

# Thay đổi status thành RESERVED
curl -X PATCH "http://localhost:8080/api/items/ITM00000000000001/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "RESERVED"
  }'

# Thay đổi status thành SOLD
curl -X PATCH "http://localhost:8080/api/items/ITM00000000000001/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "SOLD"
  }'

# Thay đổi status thành HIDDEN
curl -X PATCH "http://localhost:8080/api/items/ITM00000000000001/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "HIDDEN"
  }'
```

#### Response

```json
{
  "itemId": "ITM00000000000001",
  "title": "iPhone 13 Pro Max - Giá Cực Rẻ",
  "status": "AVAILABLE",
  "updatedAt": "2024-01-15T14:35:00",
  ...
}
```

---

## 🗑️ 4. DELETE ENDPOINT

### Xóa Tin Đăng

#### CURL

```bash
curl -X DELETE "http://localhost:8080/api/items/ITM00000000000001" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Response (200 OK)

```json
{
  "message": "Item deleted successfully",
  "success": true
}
```

#### Response (400 Bad Request - Không phải chủ sở hữu)

```json
{
  "timestamp": "2024-01-15T14:40:00",
  "status": 400,
  "error": "Bad Request",
  "message": "You do not have permission to delete this item",
  "path": "/api/items/ITM00000000000001"
}
```

---

## ❤️ 5. FAVORITE ENDPOINTS

### 5.1 Thêm Tin Vào Danh Sách Yêu Thích

#### CURL

```bash
curl -X POST "http://localhost:8080/api/items/ITM00000000000001/favorite" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Response

```json
{
  "message": "Item added to favorites",
  "success": true
}
```

---

### 5.2 Xóa Tin Khỏi Danh Sách Yêu Thích

#### CURL

```bash
curl -X DELETE "http://localhost:8080/api/items/ITM00000000000001/favorite" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Response

```json
{
  "message": "Item removed from favorites",
  "success": true
}
```

---

## 💳 6. PAYMENT CALLBACK ENDPOINT

### Xử Lý VNPay Callback

#### CURL

```bash
# VNPay sẽ redirect browser tới URL này
curl -X GET "http://localhost:8080/api/items/payment-callback?vnp_Amount=2500000000&vnp_BankCode=NCB&vnp_BankTranNo=123456789&vnp_CardType=DEBIT&vnp_OrderInfo=ItemITM00000000000001&vnp_PayDate=20240115103045&vnp_ResponseCode=00&vnp_TmnCode=SHOP123&vnp_TransactionNo=VNP20240115103045&vnp_TransactionStatus=0&vnp_TxnRef=VNP20240115103045ABC&vnp_SecureHash=abc123def456..."
```

#### Response (Success - 302 Redirect)

```
Location: http://localhost:3000/payment-success?status=success&transactionId=VNP20240115103045
```

#### Response (Failure - 302 Redirect)

```
Location: http://localhost:3000/payment-failed?status=error&message=Payment%20verification%20failed
```

---

## 🔄 Common Error Responses

### 400 Bad Request

```json
{
  "timestamp": "2024-01-15T14:45:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Item title is required",
  "path": "/api/items"
}
```

### 404 Not Found

```json
{
  "timestamp": "2024-01-15T14:45:00",
  "status": 404,
  "error": "Not Found",
  "message": "Item not found with id: ITM00000000000999",
  "path": "/api/items/ITM00000000000999"
}
```

### 401 Unauthorized

```json
{
  "timestamp": "2024-01-15T14:45:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or missing JWT token",
  "path": "/api/items/me"
}
```

### 403 Forbidden

```json
{
  "timestamp": "2024-01-15T14:45:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to update this item",
  "path": "/api/items/ITM00000000000002"
}
```

---

## 📚 Postman Collection

Bạn có thể import collection JSON này vào Postman:

```json
{
  "info": {
    "name": "Items API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Item (Multipart)",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/items",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          }
        ],
        "body": {
          "mode": "formdata"
        }
      }
    },
    {
      "name": "Get All Items",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/items"
      }
    },
    {
      "name": "Get Item By ID",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/items/ITM00000000000001"
      }
    },
    {
      "name": "Update Item",
      "request": {
        "method": "PUT",
        "url": "http://localhost:8080/api/items/ITM00000000000001",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ]
      }
    },
    {
      "name": "Delete Item",
      "request": {
        "method": "DELETE",
        "url": "http://localhost:8080/api/items/ITM00000000000001",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          }
        ]
      }
    }
  ]
}
```

---

## 🧪 Postman Variables

Thêm vào Environment/Collection variables:

```json
{
  "jwt_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "item_id": "ITM00000000000001",
  "category_id": "CAT001",
  "user_id": "USR00000000000001",
  "base_url": "http://localhost:8080"
}
```

---

**Last Updated**: January 2024
