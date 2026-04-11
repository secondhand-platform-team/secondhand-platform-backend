# Payment Verification Service - Usage Guide

## Overview

The payment verification functionality allows `core-service` to communicate with `order-service` via gRPC to handle payment operations including VN Pay payment creation, verification, and status checking.

## Features

- **Create VN Pay Payment**: Initiate a payment for an item
- **Verify Payment Callback**: Verify payment from VN Pay callback parameters
- **Get Payment Status**: Retrieve the status of a completed payment

## Architecture

```
core-service (gRPC Client)
        ↓ (gRPC Calls via Port 9093)
order-service (gRPC Server)
        ↓
PaymentGrpcService
        ↓
PaymentService (Business Logic)
```

## Implementation Components

### 1. PaymentEventService (Interface)

**Location**: `com.secondhand.coreservice.service.PaymentEventService`

Defines three methods for payment operations:

```java
CreatePaymentResponse createVnPayPayment(Long amount, String itemId, String bankCode, String language, String userId);
VerifyPaymentResponse verifyPaymentCallback(String transactionId, String orderId, String responseCode, String secureHash);
GetPaymentStatusResponse getPaymentStatus(String orderId, String paymentId);
```

### 2. PaymentEventServiceImpl (Implementation)

**Location**: `com.secondhand.coreservice.service.impl.PaymentEventServiceImpl`

Implements the PaymentEventService interface and uses PaymentGrpcClient for gRPC communication.

### 3. PaymentGrpcClient (gRPC Client)

**Location**: `com.secondhand.coreservice.grpc.PaymentGrpcClient`

Handles low-level gRPC communication with order-service:

- Injected as a Spring bean with `@GrpcClient("order-service")`
- Uses BlockingStub for synchronous calls
- Comprehensive error handling and logging

## Configuration

### gRPC Client Configuration (application.yml)

```yaml
grpc:
  client:
    order-service:
      address: "static://localhost:9093" # Docker: 'static://order-service:9093'
      negotiation-type: "plaintext"
      enable-keep-alive: true
      keep-alive-without-calls: true
      keep-alive-time: 30s
```

### Dependencies

Both services already have required dependencies:

- `grpc-client-spring-boot-starter` (core-service)
- `grpc-server-spring-boot-starter` (order-service)
- `grpc-protobuf` and `grpc-stub`

## Usage Example

### Injecting the Service

```java
@Service
public class MyPaymentService {

    @Autowired
    private PaymentEventService paymentEventService;

    // Use the service methods...
}
```

### Verify Payment Callback

```java
public void handlePaymentCallback(String transactionId, String orderId,
                                 String responseCode, String secureHash) {
    try {
        VerifyPaymentResponse response = paymentEventService.verifyPaymentCallback(
            transactionId,
            orderId,
            responseCode,
            secureHash
        );

        if (response.getIsValid()) {
            log.info("Payment verified successfully: {}", response.getMessage());
            // Update order status to PAID
        } else {
            log.warn("Payment verification failed: {}", response.getMessage());
            // Mark payment as failed
        }
    } catch (RuntimeException e) {
        log.error("Error verifying payment", e);
        // Handle error - retry or notify user
    }
}
```

### Get Payment Status

```java
public PaymentStatus getOrderPaymentStatus(String orderId, String paymentId) {
    GetPaymentStatusResponse response = paymentEventService.getPaymentStatus(orderId, paymentId);

    String status = response.getStatus();
    long amount = response.getAmount();
    String method = response.getMethod();

    return new PaymentStatus(status, amount, method);
}
```

### Create Payment

```java
public String initiateItemPayment(String itemId, long priceInVND, String userId) {
    CreatePaymentResponse response = paymentEventService.createVnPayPayment(
        priceInVND,
        itemId,
        "NCB",           // Bank code
        "vn",            // Language
        userId
    );

    if ("00".equals(response.getCode())) {
        return response.getPaymentUrl();  // Redirect user to VN Pay
    } else {
        throw new RuntimeException("Failed to create payment: " + response.getMessage());
    }
}
```

## Proto Definitions

### Messages Used

```protobuf
message VerifyPaymentRequest {
  string transactionId = 1;
  string orderId = 2;
  string responseCode = 3;
  string secureHash = 4;
}

message VerifyPaymentResponse {
  bool isValid = 1;
  string status = 2;
  string message = 3;
}
```

## Error Handling

All methods throw `RuntimeException` with descriptive messages:

- "Failed to create VN Pay payment"
- "Failed to verify payment callback"
- "Failed to get payment status"

The underlying gRPC client catches `StatusRuntimeException` for gRPC errors.

## Logging

All operations are logged at INFO and ERROR levels:

- Operation start with parameters
- Success with result code/status
- Errors with full exception details

## Testing

### Local Testing

- Ensure order-service is running on port 9093
- Ensure core-service points to correct gRPC address
- Check logs for gRPC connection errors

### Docker Testing

- Update gRPC address to: `'static://order-service:9093'`
- Ensure services can resolve hostname

## Future Enhancements

- Add async/streaming support (AsyncStub)
- Add circuit breaker pattern for resilience
- Add metrics and monitoring
- Add caching for payment status queries
