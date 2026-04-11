# VN Pay Integration Guide

## Overview

This document describes the complete VN Pay payment integration for the Secondhand Platform Backend.

## Components

### 1. DTOs (Data Transfer Objects)

#### CreatePaymentRequest

Located in: `dto/request/CreatePaymentRequest.java`

```java
{
  "amount": 100000,           // Amount in VND
  "orderId": "ORDER_123",     // Order ID
  "bankCode": "NCB",          // (Optional) Bank code
  "language": "vn"            // (Optional) Language
}
```

#### PaymentResponse

Located in: `dto/response/PaymentResponse.java`

```java
{
  "code": "00",              // "00" = success, "99" = error
  "message": "success",      // Message
  "data": "payment_url"      // VN Pay payment URL or error info
}
```

#### PaymentCallbackResponse

Located in: `dto/response/PaymentCallbackResponse.java`

- Contains callback information from VN Pay
- Used to handle payment return/verification

### 2. Models

#### Payment Entity

- Fields: id, amount, method, status, paidAt, createdAt, order, transaction
- Methods: PaymentMethod.VNPAY, PaymentStatus (PENDING/PAID/FAILED/REFUNDED)
- Relationships: One-to-One with Order

#### PaymentMethod Enum

- Added: VNPAY (in addition to COD, BANK_TRANSFER, MOMO)

### 3. Service Layer

#### PaymentService Interface

Methods:

- `createVnPayPayment()` - Create payment URL
- `handleVnPayReturn()` - Process return from VN Pay
- `verifyVnPayCallback()` - Verify payment callback signature

#### PaymentServiceImpl

Complete implementation of PaymentService with:

- Payment URL generation with secure hash
- Payment verification
- Callback handling
- Payment record persistence

### 4. Controller

#### PaymentController

Endpoints:

**Create Payment**

- POST `/api/payment/create_payment`
- Request: CreatePaymentRequest
- Response: PaymentResponse with payment URL
- Example:

```bash
POST http://localhost:8081/api/payment/create_payment
Content-Type: application/json

{
  "amount": 100000,
  "orderId": "order_001",
  "bankCode": "NCB"
}
```

**Return/Callback Handler**

- GET `/api/payment/vnpay_return`
- Handles redirect from VN Pay
- Verifies payment callback
- Updates payment status

### 5. Configuration

#### VnPayConfig

Located in: `config/VnPayConfig.java`

```java
public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
public static String vnp_ReturnUrl = "http://localhost:3000/payment-callback";
public static String vnp_TmnCode = "Q7T511E4";
public static String secretKey = "8V4FDJROI38BXRSYI98AYFW8F0H4R20M";
public static String vnp_ApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
```

**Important:** Update these values with your actual VN Pay credentials!

Utility methods:

- `getRandomNumber()` - Generate random transaction reference
- `getIpAddress()` - Extract client IP from request
- `hmacSHA512()` - Generate secure hash
- `hashAllFields()` - Hash all VN Pay fields for verification
- `md5()`, `Sha256()` - Hash functions for additional security

### 6. Repository

#### PaymentRepository

Extends JpaRepository<Payment, String>

- Methods: save(), findById(), findByOrderId()
- Auto-generated CRUD operations

## Payment Flow

```
Client App
    |
    v
POST /api/payment/create_payment
    |
    v
PaymentController.createPayment()
    |
    v
PaymentService.createVnPayPayment()
    |
    +-- Generate payment parameters
    +-- Create secure hash (HMAC SHA512)
    +-- Save payment record (PENDING status)
    +-- Generate payment URL
    |
    v
Return PaymentResponse with VN Pay URL
    |
    v
Redirect user to VN Pay payment page
    |
    v
User completes payment
    |
    v
VN Pay redirects to /api/payment/vnpay_return
    |
    v
PaymentController.vnpayReturn()
    |
    v
PaymentService.verifyVnPayCallback()
    |
    +-- Verify signature with secret key
    |
    v
PaymentService.handleVnPayReturn()
    |
    +-- Update payment status (PAID/FAILED)
    +-- Update order status
    |
    v
Return status to Client
```

## Security Considerations

1. **Secure Hash (HMAC SHA512)**:
   - Used to verify all payment parameters
   - Prevents tampering and unauthorized requests

2. **Secret Key Protection**:
   - Keep `secretKey` confidential
   - Never expose in client-side code
   - Store in environment variables in production

3. **Callback Verification**:
   - Always verify the secure hash in callback
   - Check response code before updating payment status
   - Validate amount and order information

4. **IP Address Validation**:
   - Extracts client IP for VN Pay security
   - Supports proxies (X-FORWARDED-FOR header)

## Environment Variables

In production, update these in your environment/application.properties:

```properties
vnpay.tmn-code=YOUR_TMN_CODE
vnpay.secret-key=YOUR_SECRET_KEY
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html  # prod: https://vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=YOUR_PRODUCTION_URL/api/payment/vnpay_return
vnpay.api-url=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction  # prod: https://vnpayment.vn/merchant_webapi/api/transaction
```

## Testing

### Test Credentials

- TMN Code: Q7T511E4
- Secret Key: 8V4FDJROI38BXRSYI98AYFW8F0H4R20M
- Environment: Sandbox (testing only)

### VN Pay Sandbox Test Cards

1. **Successful Payment**:
   - Card: 9704198526191432198
   - OTP: 123456
   - CVV: 123

2. **Failed Payment**:
   - Use invalid OTP or incorrect CVV

## Bank Codes (Optional)

Common Vietnamese bank codes:

- NCB: National Commercial Bank
- AGRIBANK: Vietnam Bank for Agriculture and Rural Development
- SACOMBANK: Saigon Commercial Bank
- VIETCOMBANK: Bank for Foreign Trade of Vietnam
- BIDV: Bank for Investment and Development of Vietnam
- TPB: Techcombank
- ACB: Asia Commercial Bank
- VIB: Vietnam International Bank
- SHB: SHB Bank

## Error Handling

Response codes from VN Pay:

- `00` - Success
- `01` - Failed
- `02` - Chargeback
- `03` - Refund
- `99` - Unknown error

## Dependencies

Required Maven dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- postgresql (database)
- gson (JSON processing)
- lombok (annotations)

## Future Enhancements

1. **Refund Support**: Implement VN Pay refund API
2. **Recurring Payments**: Subscription payment support
3. **Payment History**: Query transaction history from VN Pay
4. **Webhook Support**: Handle server-to-server notifications
5. **Multiple Payment Methods**: Extend to support other e-wallets
6. **Rate Limiting**: Add rate limits to payment endpoints
7. **Payment Analytics**: Track payment metrics and patterns

## Support

For more information about VN Pay:

- Documentation: https://sandbox.vnpayment.vn/
- Support: support@vnpayment.vn
- Sandbox Dashboard: https://sandbox.vnpayment.vn/
