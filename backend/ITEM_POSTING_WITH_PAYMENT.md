# Item Posting with Payment Verification - Implementation Guide

## Overview

Item posting now requires payment verification before an item can be successfully created. This ensures that users must complete payment before publishing their items.

## Flow Diagram

```
1. Client initiates payment
   └─→ POST /api/payments/create (to order-service)
       └─→ Receives transactionId, paymentUrl

2. User completes VN Pay payment
   └─→ VN Pay redirects with responseCode, secureHash

3. Client posts item with payment verification
   └─→ POST /api/items or /api/items/json
       └─→ Sends itemData + payment info
       └─→ core-service verifies via gRPC
       └─→ If valid → Item created ✅
       └─→ If invalid → Error response ❌
```

## Changes Made

### 1. ItemRequest DTO - Added Payment Fields

**File**: `core-service/src/main/java/com/secondhand/coreservice/dto/request/ItemRequest.java`

New fields added:

```java
// Payment verification fields
private String transactionId;      // VN Pay transaction ID
private String orderId;            // Order ID from payment service
private String responseCode;       // VN Pay response code
private String secureHash;         // VN Pay secure hash for verification
```

### 2. ItemServiceImpl - Added Payment Verification

**File**: `core-service/src/main/java/com/secondhand/coreservice/service/impl/ItemServiceImpl.java`

#### Dependency Injection

Added `PaymentEventService` to verify payments via gRPC:

```java
private final PaymentEventService paymentEventService;
```

#### Payment Verification Method

New method `verifyPaymentBeforeCreatingItem()`:

- Checks if payment information is provided
- If provided, validates all required fields
- Calls `paymentEventService.verifyPaymentCallback()` via gRPC
- Throws `BadRequestException` if payment verification fails
- Logs all payment verification attempts

#### Integration

Payment verification is called at the start of `createItemInternal()`:

```java
private ItemResponse createItemInternal(ItemRequest request) {
    log.debug("Starting internal item creation process");

    // Verify payment BEFORE creating item
    verifyPaymentBeforeCreatingItem(request);

    // Continue with item creation...
}
```

## API Usage

### Step 1: Create Payment (order-service)

```bash
curl -X POST http://localhost:8083/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500000,
    "itemId": "item-123",
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

### Step 2: Complete VN Pay Payment

User visits `paymentUrl` and completes payment. VN Pay redirects back with:

- `vnp_ResponseCode`: "00" for success
- `vnp_SecureHash`: Hash for verification
- Other VN Pay response parameters

### Step 3: Post Item with Payment Verification

#### Using multipart/form-data:

```bash
curl -X POST http://localhost:8082/api/items \
  -H "Authorization: Bearer <token>" \
  -F "item=@item.json" \
  -F "images=@image1.jpg" \
  -F "images=@image2.jpg"
```

**item.json** (with payment verification):

```json
{
  "title": "iPhone 13 Pro",
  "description": "Như mới, đầy đủ phụ kiện",
  "categoryId": "cat-123",
  "price": 15000000,
  "condition": "LIKE_NEW",
  "transactionType": "SELL",
  "location": {
    "address": "123 Nguyễn Huệ",
    "ward": "Bến Nghé",
    "district": "Q1",
    "city": "TP.HCM"
  },
  "attributes": [],
  "transactionId": "txn-123456",
  "orderId": "order-123",
  "responseCode": "00",
  "secureHash": "abc123def456..."
}
```

#### Using JSON endpoint:

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "iPhone 13 Pro",
    "description": "Như mới",
    "categoryId": "cat-123",
    "price": 15000000,
    "condition": "LIKE_NEW",
    "transactionId": "txn-123456",
    "orderId": "order-123",
    "responseCode": "00",
    "secureHash": "abc123def456..."
  }'
```

## Response Scenarios

### ✅ Payment Verified - Item Created Successfully

```json
{
  "itemId": "item-123",
  "title": "iPhone 13 Pro",
  "price": 15000000,
  "status": "AVAILABLE",
  "createdAt": "2026-04-06T10:30:00Z"
}
```

HTTP Status: `201 Created`

### ❌ Payment Verification Failed

```json
{
  "error": "Payment verification failed: Payment not valid"
}
```

HTTP Status: `400 Bad Request`

### ❌ Missing Payment Information

```json
{
  "error": "Complete payment information is required (transactionId, orderId, responseCode, secureHash)"
}
```

HTTP Status: `400 Bad Request`

### ❌ gRPC Communication Error

```json
{
  "error": "Payment verification error: Failed to verify payment: Connection refused"
}
```

HTTP Status: `400 Bad Request`

## Optional Payment Verification

If no payment information is provided in the request:

- Payment verification is **skipped**
- Item is created normally
- This allows for free items or items posted without immediate payment

Example (free item, no payment required):

```json
{
  "title": "Free item - give away",
  "description": "Free to anyone",
  "categoryId": "cat-123",
  "price": 0,
  "transactionType": "GIVE_AWAY"
  // No payment fields = payment verification skipped
}
```

## Error Handling

### BadRequestException Scenarios

1. **Incomplete payment data**: Missing any of the 4 required fields
2. **Payment verification failed**: Server returns invalid payment status
3. **gRPC communication error**: Cannot reach order-service
4. **Invalid payment response**: Unexpected response format

All errors are logged and returned with descriptive messages to help debugging.

## Logging

Payment verification operations are logged at INFO level:

```
INFO - No payment information provided, skipping payment verification
INFO - Verifying payment for item - TransactionId: txn-123, OrderId: order-123
INFO - Payment verified successfully for transaction: txn-123
WARN - Payment verification failed for transaction: txn-123
ERROR - Error during payment verification
```

## Configuration

### gRPC Connection

Configured in `application.yml`:

```yaml
grpc:
  client:
    order-service:
      address: "static://localhost:9093" # or 'order-service:9093' in Docker
      negotiation-type: "plaintext"
```

## Testing Scenarios

### Test 1: Valid Payment

1. Create payment through order-service
2. Complete VN Pay payment (success)
3. Post item with payment verification info
4. Expected: Item created successfully ✅

### Test 2: Invalid Payment

1. Create payment through order-service
2. Do NOT complete payment (or failed payment)
3. Post item with payment verification info
4. Expected: BadRequestException with payment verification failed message ❌

### Test 3: Missing Payment Info

1. Post item without any payment fields
2. Expected: Item created successfully (free item) ✅

### Test 4: Incomplete Payment Data

1. Post item with only transactionId (missing other fields)
2. Expected: BadRequestException requiring all fields ❌

## Future Enhancements

- Add payment amount validation (compare with item price)
- Add payment status persistence in core-service
- Add automatic item expiration if payment not received within timeframe
- Add payment retry mechanism
- Add audit logging for payment verifications
- Add metrics/monitoring for payment success rate
