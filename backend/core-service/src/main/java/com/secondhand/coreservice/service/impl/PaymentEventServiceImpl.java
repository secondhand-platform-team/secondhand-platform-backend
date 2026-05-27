package com.secondhand.coreservice.service.impl;

import com.secondhand.coreservice.client.PaymentRestClient;
import com.secondhand.coreservice.client.PaymentRestClient.PaymentCreateResult;
import com.secondhand.coreservice.client.PaymentRestClient.PaymentStatusResult;
import com.secondhand.coreservice.client.PaymentRestClient.PaymentVerifyResult;
import com.secondhand.coreservice.service.PaymentEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventServiceImpl implements PaymentEventService {

    private final PaymentRestClient paymentRestClient;

    @Override
    public PaymentCreateResult createVnPayPayment(Long amount, String bankCode, String language, String userId, String walletCallbackUrl) {
        log.info("Creating VN Pay payment - Amount: {}, BankCode: {}, Language: {}, UserId: {}",
                amount, bankCode, language, userId);
        return paymentRestClient.createVnPayPayment(amount, bankCode, language, userId,walletCallbackUrl);
    }

    @Override
    public PaymentVerifyResult verifyPaymentCallback(String transactionId, String orderId,
            String responseCode, String secureHash) {
        log.info("Verifying payment callback - TransactionId: {}, OrderId: {}", transactionId, orderId);
        return paymentRestClient.verifyPaymentCallback(transactionId, orderId, responseCode, secureHash);
    }

    @Override
    public PaymentStatusResult getPaymentStatus(String transactionId) {
        log.info("Getting payment status - TransactionId: {}", transactionId);
        return paymentRestClient.getPaymentStatus(transactionId);
    }

    @Override
    public void updatePaymentStatus(String transactionId, String status) {
        log.info("Updating payment status - TransactionId: {}, Status: {}", transactionId, status);
        paymentRestClient.updatePaymentStatus(transactionId, status);
    }
}

