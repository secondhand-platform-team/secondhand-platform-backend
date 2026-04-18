# Free Item Posting with Free Sell Limit - Implementation Guide

## Overview

Implemented logic to allow users to post items for free when they have remaining `freeSellUsed` quota. Once the quota is exhausted, users must pay to post items.

## Changes Made

### 1. **Auth Service** - Added method to get freeSellUsed count

**File**: `auth-service/src/main/java/com/secondhand/authservice/service/UserService.java`

- Added new interface method: `int getFreeSellUsed(String userId);`

**File**: `auth-service/src/main/java/com/secondhand/authservice/service/impl/UserServiceImpl.java`

- Implemented `getFreeSellUsed()` to retrieve the user's current free sell quota

**File**: `auth-service/src/main/java/com/secondhand/authservice/controller/AuthController.java`

- Added new endpoint: `GET /api/users/{userId}/free-sell-use`
- Returns the number of free posts a user still has available

### 2. **Core Service** - Updated Feign Client

**File**: `core-service/src/main/java/com/secondhand/coreservice/client/UserServiceClient.java`

- Added new method: `int getFreeSellUsed(@PathVariable String userId);`

### 3. **Core Service** - Added Free Sell Transaction Type

**File**: `core-service/src/main/java/com/secondhand/coreservice/model/enums/TransactionType.java`

- Added new enum value: `FREE_SELL`
- Transaction types now: `SELL`, `GIVE_AWAY`, `FREE_SELL`

### 4. **Core Service** - Updated Item Creation Logic

**File**: `core-service/src/main/java/com/secondhand/coreservice/service/impl/ItemServiceImpl.java`

#### Key Changes in `createItemInternal()`:

**Before (Old Logic):**

- SELL type: Always required payment
- GIVE_AWAY: Posted directly without payment

**After (New Logic):**

1. **Check Free Sell Quota**: When user posts a SELL item:
   - Get user's `freeSellUsed` count via `userServiceClient.getFreeSellUsed(userId)`
   - If `freeSellUsed > 0`: Auto-promote to `FREE_SELL`, post directly (no payment)
   - If `freeSellUsed <= 0`: Require payment verification

2. **Free Sell Item Handling**:
   - Set status to `ACTIVE` (publish immediately)
   - Decrement the user's `freeSellUsed` count via `userServiceClient.decrementFreeSellUse(userId)`
   - No payment required

3. **Paid SELL Item Handling**:
   - Set status to `DRAFT` (wait for payment)
   - Create VNPay payment request
   - User must complete payment to activate

4. **Removed Free Sell Decrement from Payment Callback**:
   - Removed `decrementFreeSellUse()` call from `handleVNPayCallback()`
   - Free sell count is now only decremented during FREE_SELL item creation
   - Paid SELL items don't consume free quota

## Flow Diagram

```
User Posts Item (SELL type)
    ↓
Check freeSellUsed count
    ├─ freeSellUsed > 0?
    │   ├─ YES → Convert to FREE_SELL
    │   │   ├─ Create item as ACTIVE
    │   │   ├─ Decrement freeSellUsed counter
    │   │   └─ Return: Item published immediately ✓
    │   │
    │   └─ NO → Require payment
    │       ├─ Verify payment info
    │       ├─ Create item as DRAFT
    │       ├─ Generate VNPay payment URL
    │       └─ Wait for user to complete payment
    │           └─ On success → Activate item ✓
```

## API Usage Examples

### 1. Check User's Free Sell Quota

```bash
curl -X GET http://localhost:8000/api/users/{userId}/free-sell-use \
  -H "Authorization: Bearer <token>"
```

Response:

```json
2
```

(User has 2 free posts remaining)

### 2. Post Item with Automatic Free Posting

**Request**:

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "iPhone 13 Pro",
    "description": "Like new condition",
    "categoryId": "cat-123",
    "price": 15000000,
    "condition": "LIKE_NEW",
    "transactionType": "SELL",
    "location": {
      "address": "123 Nguyen Hue",
      "ward": "Ben Nghe",
      "district": "Q1",
      "city": "TPHCM"
    }
  }'
```

**Response** (if user has free quota):

```json
{
  "itemId": "item-uuid",
  "title": "iPhone 13 Pro",
  "status": "ACTIVE",
  "transactionType": "FREE_SELL",
  "createdAt": "2026-04-18T10:30:00Z"
}
```

HTTP Status: `201 Created`

### 3. Post Item with Payment (No Free Quota)

When user has `freeSellUsed = 0`, the system automatically requires payment:

**Request** (same as above):

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ ... item data ... }'
```

**Response** (if no free quota):

```json
{
  "itemId": "item-uuid",
  "title": "iPhone 13 Pro",
  "status": "DRAFT",
  "transactionType": "SELL",
  "paymentUrl": "https://sandbox.vnpayment.vn/...",
  "transactionId": "TXN-123456",
  "createdAt": "2026-04-18T10:30:00Z"
}
```

HTTP Status: `201 Created`

User must visit `paymentUrl` to complete payment. After payment, item becomes `ACTIVE`.

### 4. Post with Explicit Payment (Even with Free Quota)

If user wants to pay even though they have free quota, they can provide payment info:

```bash
curl -X POST http://localhost:8082/api/items/json \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "iPhone 13 Pro",
    "description": "Like new condition",
    "categoryId": "cat-123",
    "price": 15000000,
    "transactionType": "SELL",
    "transactionId": "TXN-123456",
    "orderId": "order-123",
    "responseCode": "00",
    "secureHash": "abc123def456...",
    ...
  }'
```

Response: Item created as `ACTIVE` (payment verified), and **free quota is NOT consumed** (only consumed on `FREE_SELL`)

## Transaction Type Behavior

| Transaction Type | Requires Payment | Status                          | Free Quota Impact | Use Case                                                         |
| ---------------- | ---------------- | ------------------------------- | ----------------- | ---------------------------------------------------------------- |
| `SELL`           | Depends on quota | DRAFT if unpaid, ACTIVE if paid | Not consumed      | Normal sale posts (auto-upgrade to FREE_SELL if quota available) |
| `FREE_SELL`      | No               | ACTIVE immediately              | -1                | Auto-created when SELL used with free quota                      |
| `GIVE_AWAY`      | No               | ACTIVE immediately              | None              | Giving away items                                                |

## User Experience

### Scenario 1: User with Free Posts Available (freeSellUsed = 2)

1. User posts SELL item without payment info
2. System checks quota → finds 2 available
3. System automatically posts as FREE_SELL
4. Item goes ACTIVE immediately ✓
5. User's quota becomes: freeSellUsed = 1

### Scenario 2: User Out of Free Posts (freeSellUsed = 0)

1. User posts SELL item without payment info
2. System checks quota → finds 0 available
3. System requires payment
4. User receives payment URL
5. User completes payment
6. Item goes ACTIVE ✓
7. User's quota stays: freeSellUsed = 0 (not consumed)

### Scenario 3: User Posts GIVE_AWAY

1. User posts with transactionType = "GIVE_AWAY"
2. System posts directly as ACTIVE ✓
3. Free quota is NOT affected

## Backend Response Logging

New/updated log messages:

```
INFO - User {userId} has {count} free sell uses available, allowing free posting
INFO - User {userId} has no free sell uses, payment verification required
INFO - Decrementing free sell use for user: {userId}
INFO - Free sell use decremented successfully for user: {userId}
ERROR - Error checking free sell uses for user: {userId}
ERROR - Error decrementing free sell use for user: {userId}
```

## Database Changes

No database schema changes needed:

- Uses existing `User.freeSellUsed` field
- Uses existing item posting endpoints
- New transaction type `FREE_SELL` is added to enum

## Error Handling

### Successful Cases:

- ✅ User has quota → Posts as FREE_SELL, item ACTIVE
- ✅ User no quota + valid payment → Posts as SELL, item ACTIVE
- ✅ User posts GIVE_AWAY → Item ACTIVE

### Error Cases:

1. **Cannot fetch free quota** → Fall back to payment verification
2. **Cannot decrement quota** → Log warning, item still created
3. **Payment verification fails** → Return 400 error

## Testing Checklist

- [ ] User with `freeSellUsed > 0` can post without payment
- [ ] Item status is `ACTIVE` after free posting
- [ ] `freeSellUsed` counter decrements after free post
- [ ] User with `freeSellUsed = 0` must provide payment
- [ ] Payment verification still works for quota-exhausted users
- [ ] GIVE_AWAY items don't affect quota
- [ ] Error handling when auth-service is unavailable

## Backward Compatibility

- Existing SELL/GIVE_AWAY behavior preserved
- Endpoint changes are backward compatible
- Only **behavior** changes for SELL type (auto-free if quota available)
- Client applications don't need to change (unless they want to check remaining quota)
