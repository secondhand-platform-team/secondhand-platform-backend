package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.response.PaymentResponse;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import com.secondhand.orderservice.repository.PaymentRepository;
import com.secondhand.orderservice.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/internal/payments")
@RequiredArgsConstructor
@Slf4j
public class InternalPaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPayment(@Valid @RequestBody CreateRequest request) {
        log.info("Internal createPayment - amount={}, userId={}", request.amount(), request.userId());
        PaymentResponse response = paymentService.createVnPayPaymentInternal(
                request.amount(), request.bankCode(), request.language(), request.userId());
        return ResponseEntity.ok(Map.of(
                "code", response.getCode(),
                "message", response.getMessage(),
                "paymentUrl", response.getPaymentUrl() != null ? response.getPaymentUrl() : "",
                "transactionId", response.getTransactionId() != null ? response.getTransactionId() : ""
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@Valid @RequestBody VerifyRequest request) {
        boolean valid = "00".equals(request.responseCode());
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "status", valid ? "SUCCESS" : "FAILED",
                "message", valid ? "Payment verified successfully" : "Payment verification failed"
        ));
    }

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String transactionId) {
        Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            return ResponseEntity.ok(Map.of(
                    "paymentId", payment.getId(),
                    "transactionId", payment.getTransactionId(),
                    "status", payment.getStatus().toString(),
                    "amount", payment.getAmount().longValue(),
                    "method", payment.getMethod().toString(),
                    "paidAt", payment.getPaidAt() != null ? payment.getPaidAt().toString() : ""
            ));
        }
        return ResponseEntity.ok(Map.of("status", "NOT_FOUND"));
    }

    @PatchMapping("/{transactionId}/status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable String transactionId,
            @Valid @RequestBody UpdateStatusRequest request) {
        paymentService.updatePaymentStatus(transactionId, request.status());
        return ResponseEntity.ok(Map.of("success", true, "message", "Payment status updated successfully"));
    }

    public record CreateRequest(
            @NotNull Long amount,
            String bankCode,
            String language,
            @NotBlank String userId
    ) {}

    public record VerifyRequest(
            @NotBlank String transactionId,
            String orderId,
            @NotBlank String responseCode,
            String secureHash
    ) {}

    public record UpdateStatusRequest(@NotBlank String status) {}
}
