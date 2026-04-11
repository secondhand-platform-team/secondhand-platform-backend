package com.secondhand.coreservice.service.impl;

import com.secondhand.coreservice.grpc.PaymentGrpcClient;
import com.secondhand.coreservice.grpc.payment.*;
import com.secondhand.coreservice.service.PaymentEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventServiceImpl implements PaymentEventService {

    private final PaymentGrpcClient paymentGrpcClient;

    @Override
    public CreatePaymentResponse createVnPayPayment(Long amount, String bankCode, String language,
            String userId) {
        try {
            log.info("Creating VN Pay payment - Amount: {}, BankCode: {}, Language: {}, UserId: {}",
                    amount, bankCode, language, userId);

            CreatePaymentRequest request = CreatePaymentRequest.newBuilder()
                    .setAmount(amount)
                    .setBankCode(bankCode != null ? bankCode : "NCB")
                    .setLanguage(language != null ? language : "vn")
                    .setUserId(userId != null ? userId : "")
                    .build();

            CreatePaymentResponse response = paymentGrpcClient.createVnPayPayment(request);
            log.info("VN Pay payment created successfully - Code: {}, TransactionId: {}",
                    response.getCode(), response.getTransactionId());
            return response;
        } catch (Exception e) {
            log.error("Error creating VN Pay payment", e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    @Override
    public VerifyPaymentResponse verifyPaymentCallback(String transactionId, String orderId, String responseCode,
            String secureHash) {
        try {
            log.info("Verifying payment callback - TransactionId: {}, OrderId: {}", transactionId, orderId);

            VerifyPaymentRequest request = VerifyPaymentRequest.newBuilder()
                    .setTransactionId(transactionId != null ? transactionId : "")
                    .setOrderId(orderId != null ? orderId : "")
                    .setResponseCode(responseCode != null ? responseCode : "")
                    .setSecureHash(secureHash != null ? secureHash : "")
                    .build();

            VerifyPaymentResponse response = paymentGrpcClient.verifyPaymentCallback(request);
            log.info("Payment callback verified - IsValid: {}", response.getIsValid());
            return response;
        } catch (Exception e) {
            log.error("Error verifying payment callback", e);
            throw new RuntimeException("Failed to verify payment: " + e.getMessage(), e);
        }
    }

    @Override
    public GetPaymentStatusResponse getPaymentStatus(String transactionId) {
        try {
            log.info("Getting payment status - TransactionId: {}", transactionId);

            GetPaymentStatusRequest request = GetPaymentStatusRequest.newBuilder()
                    .setOrderId(transactionId != null ? transactionId : "")
                    .build();

            GetPaymentStatusResponse response = paymentGrpcClient.getPaymentStatus(request);
            log.info("Payment status retrieved - Status: {}", response.getStatus());
            return response;
        } catch (Exception e) {
            log.error("Error getting payment status", e);
            throw new RuntimeException("Failed to get payment status: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePaymentStatus(String transactionId, String status) {
        try {
            log.info("Updating payment status - TransactionId: {}, Status: {}", transactionId, status);

            UpdatePaymentStatusRequest request = UpdatePaymentStatusRequest.newBuilder()
                    .setTransactionId(transactionId != null ? transactionId : "")
                    .setStatus(status != null ? status : "SUCCESS")
                    .build();

            UpdatePaymentStatusResponse response = paymentGrpcClient.updatePaymentStatus(request);
            log.info("Payment status updated - Success: {}", response.getSuccess());
        } catch (Exception e) {
            log.error("Error updating payment status", e);
            throw new RuntimeException("Failed to update payment status: " + e.getMessage(), e);
        }
    }
}
