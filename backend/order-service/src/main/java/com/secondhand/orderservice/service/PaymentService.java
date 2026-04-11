package com.secondhand.orderservice.service;

import com.secondhand.orderservice.dto.request.CreatePaymentRequest;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    PaymentResponse createVnPayPayment(CreatePaymentRequest request, HttpServletRequest httpRequest);

    String handleVnPayReturn(HttpServletRequest request);

    Boolean verifyVnPayCallback(HttpServletRequest request);

    void updatePaymentStatus(String transactionId, String status);
}
