package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.response.AdminPaymentResponse;
import com.secondhand.orderservice.service.PaymentService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<Page<AdminPaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) com.secondhand.orderservice.model.enums.PaymentStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate) {

        Pageable pageable = PageRequest.of(page, size);
        
        Page<AdminPaymentResponse> payments = paymentService.getAllPayments(pageable, status, startDate, endDate);
        return ResponseEntity.ok(payments);
    }



    @GetMapping("/{id}")
    public ResponseEntity<AdminPaymentResponse> getPaymentById(@PathVariable String id) {
        AdminPaymentResponse payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }
}

