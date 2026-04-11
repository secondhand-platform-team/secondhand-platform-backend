package com.secondhand.coreservice.service;

import com.secondhand.coreservice.grpc.payment.CreatePaymentResponse;
import com.secondhand.coreservice.grpc.payment.GetPaymentStatusResponse;
import com.secondhand.coreservice.grpc.payment.VerifyPaymentResponse;

public interface PaymentEventService {

    /**
     * Create a VN Pay payment request (called after item is created as DRAFT)
     * 
     * @param amount   Payment amount
     * @param bankCode Bank code (optional)
     * @param language Language preference
     * @param userId   User ID making the payment
     * @return CreatePaymentResponse containing paymentUrl and transactionId
     */
    CreatePaymentResponse createVnPayPayment(Long amount, String bankCode, String language, String userId);

    /**
     * Verify payment callback after user completes payment
     */
    VerifyPaymentResponse verifyPaymentCallback(String transactionId, String orderId, String responseCode,
            String secureHash);

    /**
     * Get payment status to check if payment is complete
     */
    GetPaymentStatusResponse getPaymentStatus(String transactionId);

    /**
     * Update payment status in order-service
     */
    void updatePaymentStatus(String transactionId, String status);
}
