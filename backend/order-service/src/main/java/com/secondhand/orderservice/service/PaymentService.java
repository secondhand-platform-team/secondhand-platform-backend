package com.secondhand.orderservice.service;

import com.secondhand.orderservice.dto.request.CreatePaymentRequest;
import com.secondhand.orderservice.dto.response.AdminPaymentResponse;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PaymentService {
    PaymentResponse createVnPayPayment(CreatePaymentRequest request, HttpServletRequest httpRequest);

    PaymentResponse createVnPayPaymentInternal(Long amount, String bankCode, String language, String userId, String returnUrl);

    String handleVnPayReturn(HttpServletRequest request);

    Boolean verifyVnPayCallback(HttpServletRequest request);

    void updatePaymentStatus(String transactionId, String status);

    Page<AdminPaymentResponse> getAllPayments(Pageable pageable, com.secondhand.orderservice.model.enums.PaymentStatus status, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    AdminPaymentResponse getPaymentById(String id);
}



