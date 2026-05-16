package com.secondhand.coreservice.service;

import com.secondhand.coreservice.client.PaymentRestClient.PaymentCreateResult;
import com.secondhand.coreservice.client.PaymentRestClient.PaymentStatusResult;
import com.secondhand.coreservice.client.PaymentRestClient.PaymentVerifyResult;

public interface PaymentEventService {

    PaymentCreateResult createVnPayPayment(Long amount, String bankCode, String language, String userId, String walletCallbackUrl);

    PaymentVerifyResult verifyPaymentCallback(String transactionId, String orderId, String responseCode,
            String secureHash);

    PaymentStatusResult getPaymentStatus(String transactionId);

    void updatePaymentStatus(String transactionId, String status);
}
