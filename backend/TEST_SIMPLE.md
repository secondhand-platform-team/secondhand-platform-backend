# Test Đơn Giản: Đăng Bài Có Thanh Toán

## Chuẩn Bị (Chạy các lệnh này 1 lần)

### Terminal 1: Core Service

```bash
cd core-service
mvn spring-boot:run
```

### Terminal 2: Order Service

```bash
cd order-service
mvn spring-boot:run
```

### Terminal 3: Auth Service

```bash
cd auth-service
mvn spring-boot:run
```

---

## Test Scenario: Đăng Bài Có Thanh Toán

### Bước 1: Lấy Token (Login)

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Response:** Sẽ có `token`

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Lưu token vào biến:**

```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Bước 2: Đăng Bài CO Payment Verification

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "iPhone 13 Pro",
    "description": "Đẹp như mới",
    "categoryId": "electronics-1",
    "price": 15000000,
    "condition": "LIKE_NEW",
    "transactionType": "SELL",
    "transactionId": "txn-20260406-001",
    "orderId": "order-20260406-001",
    "responseCode": "00",
    "secureHash": "valid_hash_123"
  }'
```

---

## Kỳ Vọng

### ✅ Nếu Thanh Toán Hợp Lệ (responseCode="00")

**Response (Status 201):**

```json
{
  "itemId": "item-xyz123",
  "title": "iPhone 13 Pro",
  "description": "Đẹp như mới",
  "price": 15000000,
  "status": "AVAILABLE",
  "userId": "user-123",
  "createdAt": "2026-04-06T10:30:00Z"
}
```

**Console Log:**

```
INFO - Creating item: iPhone 13 Pro with 0 images
INFO - Verifying payment for item - TransactionId: txn-20260406-001, OrderId: order-20260406-001
INFO - Payment verified successfully for transaction: txn-20260406-001
INFO - Item created successfully with id: item-xyz123
```

✅ **BÀI ĐĂNG THÀNH CÔNG** → Item được tạo trong database

---

### ❌ Nếu Thanh Toán Thất Bại (responseCode!="00")

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "iPhone 13 Pro",
    "categoryId": "electronics-1",
    "price": 15000000,
    "transactionId": "txn-20260406-002",
    "orderId": "order-20260406-002",
    "responseCode": "01",
    "secureHash": "invalid_hash"
  }'
```

**Response (Status 400):**

```json
{
  "error": "Payment verification failed: Payment not valid"
}
```

**Console Log:**

```
INFO - Creating item: iPhone 13 Pro with 0 images
INFO - Verifying payment for item - TransactionId: txn-20260406-002, OrderId: order-20260406-002
WARN - Payment verification failed for transaction: txn-20260406-002
ERROR - Error during payment verification
```

❌ **BÀI ĐĂNG THẤT BẠI** → Item KHÔNG được tạo, user nhận lỗi

---

## Kiểm Tra Kết Quả

### Check Database

```bash
# Connect vào PostgreSQL
psql -U postgres -d secondhand_core_db

# Xem bài viết vừa đăng
SELECT item_id, title, price, status, user_id, created_at
FROM item
ORDER BY created_at DESC
LIMIT 5;
```

**Nếu thanh toán thành công:** Item sẽ có trong list
**Nếu thanh toán thất bại:** Item KHÔNG có trong list

---

## Tóm Tắt Flow

```
1. User Login → Nhận token

2. POST /api/items/json
   + Gửi payment info (transactionId, orderId, responseCode, secureHash)

3. Core Service gọi Order Service (gRPC)
   → Verify payment

4. Nếu hợp lệ ✅
   → Tạo item
   → Return 201 + item data

5. Nếu không hợp lệ ❌
   → Không tạo item
   → Return 400 + lỗi
```

---

## Copy-Paste Sẵn (Chạy nhanh)

### Lấy Token

```bash
export TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo $TOKEN
```

### Test Thành Công

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Test Item - Success",
    "categoryId": "electronics-1",
    "price": 5000000,
    "transactionId": "txn-success-001",
    "orderId": "order-success-001",
    "responseCode": "00",
    "secureHash": "hash_valid"
  }'
```

### Test Thất Bại

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Test Item - Failed",
    "categoryId": "electronics-1",
    "price": 5000000,
    "transactionId": "txn-fail-001",
    "orderId": "order-fail-001",
    "responseCode": "01",
    "secureHash": "hash_invalid"
  }'
```

---

## Các Điều Cần Chú Ý

1. **Token:** Thay `$TOKEN` bằng token thật từ login
2. **categoryId:** Phải tồn tại trong database
3. **transactionId, orderId:** Có thể là bất kỳ string nào (không kiểm tra format)
4. **responseCode:**
   - `"00"` = Thanh toán thành công ✅
   - Bất kỳ giá trị khác = Thất bại ❌
5. **Kết quả:** Check database xem item có được tạo hay không

---

## Xem Logs Real-time

**Trong terminal chạy core-service:**

- Tìm dòng chứa "Verifying payment"
- Hoặc "Payment verified successfully"
- Hoặc "Payment verification failed"
