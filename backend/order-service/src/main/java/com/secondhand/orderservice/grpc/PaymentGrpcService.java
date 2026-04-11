package com.secondhand.orderservice.grpc;

//import com.secondhand.orderservice.dto.request.CreatePaymentRequest;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import com.secondhand.orderservice.grpc.payment.*;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.repository.PaymentRepository;
import com.secondhand.orderservice.service.PaymentService;
import io.grpc.stub.StreamObserver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PaymentGrpcService extends PaymentGrpcServiceGrpc.PaymentGrpcServiceImplBase {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Override
    public void createVnPayPayment(CreatePaymentRequest request,
            StreamObserver<CreatePaymentResponse> responseObserver) {
        try {
            log.info("Processing createVnPayPayment request for amount: {} and userId: {}",
                    request.getAmount(), request.getUserId());

            HttpServletRequest httpRequest = getHttpServletRequest();

            // Create payment request DTO
            com.secondhand.orderservice.dto.request.CreatePaymentRequest paymentDto = new com.secondhand.orderservice.dto.request.CreatePaymentRequest();
            paymentDto.setAmount(request.getAmount());
            paymentDto.setBankCode(
                    request.getBankCode() != null && !request.getBankCode().isEmpty() ? request.getBankCode() : null);
            paymentDto.setLanguage(
                    request.getLanguage() != null && !request.getLanguage().isEmpty() ? request.getLanguage() : "vn");
            paymentDto.setUserId(
                    request.getUserId() != null && !request.getUserId().isEmpty() ? request.getUserId() : null);

            PaymentResponse response = paymentService.createVnPayPayment(paymentDto, httpRequest);

            // Generate a transaction ID if not already generated
            String transactionId = response.getTransactionId() != null ? response.getTransactionId()
                    : "TXN-" + System.currentTimeMillis();

            CreatePaymentResponse grpcResponse = CreatePaymentResponse.newBuilder()
                    .setCode(response.getCode())
                    .setMessage(response.getMessage())
                    .setPaymentUrl(response.getPaymentUrl() != null ? response.getPaymentUrl() : "")
                    .setTransactionId(transactionId)
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error creating VN Pay payment", e);
            responseObserver.onError(new Exception("Failed to create payment: " + e.getMessage()));
        }
    }

    @Override
    public void verifyPaymentCallback(VerifyPaymentRequest request,
            StreamObserver<VerifyPaymentResponse> responseObserver) {
        try {
            log.info("Verifying payment callback for transaction: {}, orderId: {}",
                    request.getTransactionId(), request.getOrderId());

            // Verify payment using request parameters
            boolean isValid = verifyPaymentWithParams(
                    request.getTransactionId(),
                    request.getOrderId(),
                    request.getResponseCode(),
                    request.getSecureHash());

            String status = isValid ? "SUCCESS" : "FAILED";
            String message = isValid ? "Payment verified successfully" : "Payment verification failed";

            VerifyPaymentResponse grpcResponse = VerifyPaymentResponse.newBuilder()
                    .setIsValid(isValid)
                    .setStatus(status)
                    .setMessage(message)
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error verifying payment callback", e);
            VerifyPaymentResponse errorResponse = VerifyPaymentResponse.newBuilder()
                    .setIsValid(false)
                    .setStatus("ERROR")
                    .setMessage("Error: " + e.getMessage())
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPaymentStatus(GetPaymentStatusRequest request,
            StreamObserver<GetPaymentStatusResponse> responseObserver) {
        try {
            log.info("Getting payment status for order: {}", request.getOrderId());

            Optional<Payment> paymentOpt = paymentRepository.findById(request.getPaymentId());

            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();

                long paidAtTimestamp = payment.getPaidAt() != null
                        ? payment.getPaidAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        : 0L;

                GetPaymentStatusResponse grpcResponse = GetPaymentStatusResponse.newBuilder()
                        .setPaymentId(payment.getId())
                        .setOrderId(request.getOrderId())
                        .setStatus(payment.getStatus().toString())
                        .setAmount(payment.getAmount().longValue())
                        .setMethod(payment.getMethod().toString())
                        .setPaidAt(paidAtTimestamp)
                        .build();

                responseObserver.onNext(grpcResponse);
            } else {
                log.warn("Payment not found for ID: {}", request.getPaymentId());
                GetPaymentStatusResponse errorResponse = GetPaymentStatusResponse.newBuilder()
                        .setStatus("NOT_FOUND")
                        .build();
                responseObserver.onNext(errorResponse);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting payment status", e);
            responseObserver.onError(new Exception("Failed to get payment status: " + e.getMessage()));
        }
    }

    @Override
    public void updatePaymentStatus(UpdatePaymentStatusRequest request,
            StreamObserver<UpdatePaymentStatusResponse> responseObserver) {
        try {
            log.info("Updating payment status for transactionId: {} to status: {}",
                    request.getTransactionId(), request.getStatus());

            paymentService.updatePaymentStatus(request.getTransactionId(), request.getStatus());

            UpdatePaymentStatusResponse grpcResponse = UpdatePaymentStatusResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Payment status updated successfully")
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error updating payment status", e);
            UpdatePaymentStatusResponse errorResponse = UpdatePaymentStatusResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    /**
     * Utility method to get HttpServletRequest from RequestContextHolder
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * Verify payment using transaction parameters
     */
    private boolean verifyPaymentWithParams(String transactionId, String orderId, String responseCode,
            String secureHash) {
        try {
            // Check if payment with given transaction ID exists
            // For now, we check responseCode. In production, we would:
            // 1. Verify secure hash against VN Pay
            // 2. Check payment status in database
            // 3. Confirm payment amount matches

            log.info("Verifying payment - TransactionId: {}, ResponseCode: {}", transactionId, responseCode);

            // For testing: responseCode "00" means success
            // In production: Verify against VN Pay secure hash
            if ("00".equals(responseCode)) {
                log.info("Payment verification successful for transaction: {}", transactionId);
                return true;
            } else {
                log.warn("Payment verification failed - Response code: {}", responseCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error verifying payment parameters", e);
            return false;
        }
    }
}
