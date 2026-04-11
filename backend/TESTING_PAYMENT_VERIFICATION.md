# Testing Guide - Payment Verification for Item Posting

## Prerequisites - Setup

### 1. Start Services

Ensure all services are running:

```bash
# Terminal 1: Start core-service
cd core-service
mvn spring-boot:run
# Runs on: http://localhost:8082

# Terminal 2: Start order-service
cd order-service
mvn spring-boot:run
# Runs on: http://localhost:8083
# gRPC on: localhost:9093

# Terminal 3: Start auth-service
cd auth-service
mvn spring-boot:run
# Runs on: http://localhost:8081

# Terminal 4: PostgreSQL (if using Docker)
docker run --name postgres -e POSTGRES_PASSWORD=postgres -p 5435:5432 postgres
```

### 2. Get Authentication Token

First, register or login to get JWT token:

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Save the token for subsequent requests. Use it in header:

```bash
-H "Authorization: Bearer <token>"
```

---

## Test Scenarios

### ✅ Test 1: Valid Payment - Item Created Successfully

**Goal:** Verify that item is created when valid payment information is provided

#### Step 1: Create Payment

```bash
curl -X POST http://localhost:8083/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 15000000,
    "itemId": "item-test-1",
    "bankCode": "NCB",
    "language": "vn",
    "userId": "user-123"
  }'
```

Response:

```json
{
  "code": "00",
  "message": "success",
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "transactionId": "txn-123456"
}
```

**Save:** `transactionId` = "txn-123456"

#### Step 2: Simulate VN Pay Callback

Since we're testing locally, we need to:

1. Manually call VN Pay verify API OR
2. Mock the payment response

For testing, update database directly or use Postman to simulate:

```bash
# Get payment status to confirm it's pending
curl -X GET http://localhost:8083/api/payments/status \
  -H "Authorization: Bearer <token>"
```

#### Step 3: Post Item with Payment Verification

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "iPhone 13 Pro",
    "description": "Như mới, đầy đủ phụ kiện",
    "categoryId": "cat-electronics",
    "price": 15000000,
    "condition": "LIKE_NEW",
    "transactionType": "SELL",
    "location": {
      "address": "123 Nguyễn Huệ",
      "ward": "Bến Nghé",
      "district": "Q1",
      "city": "TP.HCM"
    },
    "transactionId": "txn-123456",
    "orderId": "order-123",
    "responseCode": "00",
    "secureHash": "valid_hash_here"
  }'
```

**Expected Response (201 Created):**

```json
{
  "itemId": "item-456",
  "title": "iPhone 13 Pro",
  "description": "Như mới, đầy đủ phụ kiện",
  "price": 15000000,
  "status": "AVAILABLE",
  "createdAt": "2026-04-06T10:30:00Z"
}
```

**Check Logs:**

```
INFO - Creating item: iPhone 13 Pro with 0 images
INFO - Verifying payment for item - TransactionId: txn-123456, OrderId: order-123
INFO - Payment verified successfully for transaction: txn-123456
INFO - Item created successfully with id: item-456
```

✅ **PASS** if:

- Response status is 201
- Item is created with correct data
- Logs show payment verified

---

### ❌ Test 2: Invalid Payment - Item NOT Created

**Goal:** Verify that item creation fails when payment is invalid

#### Setup

```bash
# Use same transactionId but invalid secureHash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Test Item",
    "categoryId": "cat-electronics",
    "price": 5000000,
    "transactionId": "txn-123456",
    "orderId": "order-123",
    "responseCode": "01",
    "secureHash": "invalid_hash"
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "error": "Payment verification failed: Payment not valid"
}
```

**Check Logs:**

```
WARN - Payment verification failed for transaction: txn-123456
ERROR - Error during payment verification
```

✅ **PASS** if:

- Response status is 400
- Error message contains "Payment verification failed"
- Item is NOT created in database

---

### ⚠️ Test 3: Missing Payment Fields - Error

**Goal:** Verify validation when payment fields are incomplete

#### Setup 1: Missing orderId

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Test Item",
    "categoryId": "cat-electronics",
    "price": 5000000,
    "transactionId": "txn-123456",
    "responseCode": "00",
    "secureHash": "hash123"
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "error": "Complete payment information is required (transactionId, orderId, responseCode, secureHash)"
}
```

#### Setup 2: Missing responseCode

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Test Item",
    "categoryId": "cat-electronics",
    "price": 5000000,
    "transactionId": "txn-123456",
    "orderId": "order-123",
    "secureHash": "hash123"
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "error": "Complete payment information is required (transactionId, orderId, responseCode, secureHash)"
}
```

✅ **PASS** if:

- All missing field tests return 400
- Error message requires complete payment info

---

### 🆓 Test 4: Free Item - No Payment Required

**Goal:** Verify that items can be posted without payment (free/give-away items)

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Free item - Give away",
    "description": "Free to anyone who wants",
    "categoryId": "cat-electronics",
    "price": 0,
    "condition": "USED",
    "transactionType": "GIVE_AWAY"
  }'
```

**Expected Response (201 Created):**

```json
{
  "itemId": "item-789",
  "title": "Free item - Give away",
  "description": "Free to anyone who wants",
  "price": 0,
  "status": "AVAILABLE",
  "createdAt": "2026-04-06T10:30:00Z"
}
```

**Check Logs:**

```
INFO - Creating item: Free item - Give away with 0 images
INFO - No payment information provided, skipping payment verification
INFO - Item created successfully with id: item-789
```

✅ **PASS** if:

- Response status is 201
- Item is created without payment
- Logs show payment verification skipped

---

### 📤 Test 5: With Images - Payment Verification

**Goal:** Test item posting with images and payment verification

#### Create test image first

Or use an existing image file.

```bash
curl -X POST http://localhost:8082/api/items \
  -H "Authorization: Bearer <token>" \
  -F "item={
    \"title\": \"iPhone with accessories\",
    \"categoryId\": \"cat-electronics\",
    \"price\": 12000000,
    \"transactionId\": \"txn-123456\",
    \"orderId\": \"order-123\",
    \"responseCode\": \"00\",
    \"secureHash\": \"hash123\"
  }" \
  -F "images=@/path/to/image1.jpg" \
  -F "images=@/path/to/image2.jpg"
```

**Expected Response (201 Created):**

```json
{
  "itemId": "item-img-1",
  "title": "iPhone with accessories",
  "title": "iPhone with accessories",
  "itemImageList": [
    {
      "id": "img-1",
      "url": "https://res.cloudinary.com/...",
      "isPrimary": true
    },
    {
      "id": "img-2",
      "url": "https://res.cloudinary.com/...",
      "isPrimary": false
    }
  ]
}
```

✅ **PASS** if:

- Images uploaded to Cloudinary
- Payment verified
- Item created with images

---

### ❌ Test 6: gRPC Connection Error

**Goal:** Test error handling when order-service is unavailable

#### Setup

1. Stop order-service: `Ctrl+C` in order-service terminal
2. Try to post item with payment verification:

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Test Item",
    "categoryId": "cat-electronics",
    "price": 5000000,
    "transactionId": "txn-123456",
    "orderId": "order-123",
    "responseCode": "00",
    "secureHash": "hash123"
  }'
```

**Expected Response (400 Bad Request):**

```json
{
  "error": "Payment verification error: Failed to verify payment: UNAVAILABLE"
}
```

**Check Logs:**

```
ERROR - gRPC call failed for verifyPaymentCallback: UNAVAILABLE
ERROR - Error during payment verification
```

✅ **PASS** if:

- Clear error message about gRPC failure
- Item is NOT created
- Service doesn't crash

---

## Test Execution in Postman

### Import Collection Template

Create file: `postman_collection.json`

```json
{
  "info": {
    "name": "Payment Verification Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Create Payment",
      "request": {
        "method": "POST",
        "url": "http://localhost:8083/api/payments/create",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "raw": "{\"amount\": 15000000, \"itemId\": \"item-test-1\", \"bankCode\": \"NCB\", \"language\": \"vn\", \"userId\": \"user-123\"}"
        }
      }
    },
    {
      "name": "2. Post Item with Valid Payment",
      "request": {
        "method": "POST",
        "url": "http://localhost:8082/api/items/json",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Authorization", "value": "Bearer {{token}}" }
        ],
        "body": {
          "raw": "{\"title\": \"iPhone 13 Pro\", \"categoryId\": \"cat-electronics\", \"price\": 15000000, \"transactionId\": \"txn-123456\", \"orderId\": \"order-123\", \"responseCode\": \"00\", \"secureHash\": \"hash123\"}"
        }
      }
    },
    {
      "name": "3. Post Item - Missing Payment Field",
      "request": {
        "method": "POST",
        "url": "http://localhost:8082/api/items/json",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Authorization", "value": "Bearer {{token}}" }
        ],
        "body": {
          "raw": "{\"title\": \"Test Item\", \"categoryId\": \"cat-electronics\", \"price\": 5000000, \"transactionId\": \"txn-123456\"}"
        }
      }
    },
    {
      "name": "4. Post Free Item (No Payment)",
      "request": {
        "method": "POST",
        "url": "http://localhost:8082/api/items/json",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Authorization", "value": "Bearer {{token}}" }
        ],
        "body": {
          "raw": "{\"title\": \"Free Item\", \"categoryId\": \"cat-electronics\", \"price\": 0, \"transactionType\": \"GIVE_AWAY\"}"
        }
      }
    }
  ]
}
```

### Steps

1. Open Postman
2. Click "Import" → Select `postman_collection.json`
3. Set Postman variable: `token` = your JWT token
4. Run each request sequentially
5. Check response and logs

---

## Database Verification

### Check Created Items

```sql
-- Connect to PostgreSQL
psql -U postgres -d secondhand_core_db

-- List all items with payment verification
SELECT id, item_id, title, price, status, created_at, user_id
FROM item
ORDER BY created_at DESC
LIMIT 10;

-- Check specific item
SELECT * FROM item WHERE item_id = 'item-456';

-- Get item count before/after tests
SELECT COUNT(*) as total_items FROM item;
```

### Check Logs in Database

If you want to log payment verifications:

```sql
-- Create audit table (optional)
CREATE TABLE payment_verification_audit (
  id UUID PRIMARY KEY,
  item_id VARCHAR(255),
  transaction_id VARCHAR(255),
  order_id VARCHAR(255),
  is_valid BOOLEAN,
  result_message TEXT,
  verified_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Log Monitoring

### Watch Logs in Real-Time

#### Terminal for core-service logs:

```bash
cd core-service
tail -f logs/*.log | grep "Payment\|Item"
```

#### Terminal for order-service logs:

```bash
cd order-service
tail -f logs/*.log | grep "Payment"
```

### Search for Payment Verification Logs

```bash
# Search in logs
grep -r "Verifying payment" core-service/logs/
grep -r "Payment verified" core-service/logs/
grep -r "Payment verification failed" core-service/logs/
```

---

## Debugging Tips

### 1. Enable Debug Logging

Add to `application.properties`:

```properties
logging.level.com.secondhand.coreservice=DEBUG
logging.level.com.secondhand.coreservice.service.impl.ItemServiceImpl=DEBUG
logging.level.com.secondhand.coreservice.grpc=DEBUG
```

### 2. Check gRPC Connection

```bash
# Test gRPC connection to order-service
grpcurl -plaintext localhost:9093 list

# Should output:
# payment.PaymentGrpcService
```

### 3. Mock Payment Verification Response

For testing without real VN Pay, you can modify `PaymentGrpcClient`:

```java
// In PaymentGrpcClient (TESTING ONLY)
@Override
public VerifyPaymentResponse verifyPaymentCallback(VerifyPaymentRequest request) {
    // TODO: Remove this before production!
    if ("TEST".equals(request.getTransactionId())) {
        return VerifyPaymentResponse.newBuilder()
            .setIsValid(true)
            .setStatus("SUCCESS")
            .setMessage("Test payment verified")
            .build();
    }
    // ... actual implementation
}
```

### 4. Exception Stack Traces

Look for complete error details:

```bash
grep -A 10 "Payment verification error" core-service/logs/app.log
```

---

## Test Result Sheet

Use this to track test results:

| Test # | Scenario               | Expected Status  | Result | Notes |
| ------ | ---------------------- | ---------------- | ------ | ----- |
| 1      | Valid payment          | 201 Created      | ⬜     |       |
| 2      | Invalid payment        | 400 Bad Request  | ⬜     |       |
| 3      | Missing orderId        | 400 Bad Request  | ⬜     |       |
| 4      | Missing responseCode   | 400 Bad Request  | ⬜     |       |
| 5      | Free item (no payment) | 201 Created      | ⬜     |       |
| 6      | With images + payment  | 201 Created      | ⬜     |       |
| 7      | gRPC unavailable       | 400 Bad Request  | ⬜     |       |
| 8      | Invalid JWT token      | 401 Unauthorized | ⬜     |       |
| 9      | Invalid category       | 400 Bad Request  | ⬜     |       |
| 10     | Price <= 0             | 400 Bad Request  | ⬜     |       |

---

## Automation Test Script

Create: `test_payment_verification.sh`

```bash
#!/bin/bash

BASE_URL="http://localhost:8082"
TOKEN="your_jwt_token_here"

echo "=== Test 1: Valid Payment ==="
curl -X POST $BASE_URL/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Test Item 1",
    "categoryId": "cat-1",
    "price": 1000000,
    "transactionId": "txn-1",
    "orderId": "order-1",
    "responseCode": "00",
    "secureHash": "hash1"
  }' \
  && echo "✅ PASS" || echo "❌ FAIL"

echo -e "\n=== Test 2: Missing Field ==="
curl -X POST $BASE_URL/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Test Item 2",
    "categoryId": "cat-1",
    "price": 1000000,
    "transactionId": "txn-2"
  }' \
  && echo "✅ PASS" || echo "❌ FAIL"

echo -e "\n=== Test 3: Free Item ==="
curl -X POST $BASE_URL/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Free Item",
    "categoryId": "cat-1",
    "price": 0
  }' \
  && echo "✅ PASS" || echo "❌ FAIL"

echo -e "\nTests completed!"
```

Run:

```bash
chmod +x test_payment_verification.sh
./test_payment_verification.sh
```

---

## Cleanup After Testing

```bash
# Delete test items from database
DELETE FROM item WHERE title LIKE '%Test%' OR title LIKE '%Free%';

# Reset item ID sequence
ALTER SEQUENCE item_id_seq RESTART WITH 1;

# Commit changes
COMMIT;
```
