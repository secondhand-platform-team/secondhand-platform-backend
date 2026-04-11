package com.secondhand.coreservice.grpc;

import com.secondhand.coreservice.grpc.payment.*;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentGrpcClient {

    @GrpcClient("order-service")
    private PaymentGrpcServiceGrpc.PaymentGrpcServiceBlockingStub paymentGrpcServiceStub;

    /**
     *
     * Create a VN Pay payment via gRPC call to order-service
     */
    public CreatePaymentResponse createVnPayPayment(CreatePaymentRequest request) {
        try {
            log.info("Calling gRPC createVnPayPayment with amount: {} for userId: {}",
                    request.getAmount(), request.getUserId());
            CreatePaymentResponse response = paymentGrpcServiceStub.createVnPayPayment(request);
            log.info("Successfully received payment URL from order-service");
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for createVnPayPayment: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to create VN Pay payment: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error in createVnPayPayment", e);
            throw new RuntimeException("Failed to create VN Pay payment: " + e.getMessage(), e);
        }
    }

    /**
     * Verify payment callback via gRPC call to order-service
     */
    public VerifyPaymentResponse verifyPaymentCallback(VerifyPaymentRequest request) {
        try {
            log.info("Calling gRPC verifyPaymentCallback for transaction: {}", request.getTransactionId());
            VerifyPaymentResponse response = paymentGrpcServiceStub.verifyPaymentCallback(request);
            log.info("Successfully verified payment callback - IsValid: {}", response.getIsValid());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for verifyPaymentCallback: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to verify payment callback: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error in verifyPaymentCallback", e);
            throw new RuntimeException("Failed to verify payment callback: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment status via gRPC call to order-service
     */
    public GetPaymentStatusResponse getPaymentStatus(GetPaymentStatusRequest request) {
        try {
            log.info("Calling gRPC getPaymentStatus for orderId: {}", request.getOrderId());
            GetPaymentStatusResponse response = paymentGrpcServiceStub.getPaymentStatus(request);
            log.info("Successfully retrieved payment status - Status: {}", response.getStatus());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for getPaymentStatus: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to get payment status: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error in getPaymentStatus", e);
            throw new RuntimeException("Failed to get payment status: " + e.getMessage(), e);
        }
    }

    /**
     * Update payment status via gRPC call to order-service
     */
    public UpdatePaymentStatusResponse updatePaymentStatus(UpdatePaymentStatusRequest request) {
        try {
            log.info("Calling gRPC updatePaymentStatus for transactionId: {} with status: {}", 
                    request.getTransactionId(), request.getStatus());
            UpdatePaymentStatusResponse response = paymentGrpcServiceStub.updatePaymentStatus(request);
            log.info("Successfully updated payment status - Success: {}", response.getSuccess());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for updatePaymentStatus: {}", e.getStatus(), e);
            throw new RuntimeException("Failed to update payment status: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error in updatePaymentStatus", e);
            throw new RuntimeException("Failed to update payment status: " + e.getMessage(), e);
        }
    }
}
