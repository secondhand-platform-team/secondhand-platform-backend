package com.secondhand.coreservice.client;

import com.secondhand.coreservice.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
@Slf4j
public class PaymentRestClient {

    private final RestClient restClient;

    public PaymentRestClient(@Value("${order.service.url}") String orderServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    public PaymentCreateResult createVnPayPayment(Long amount, String bankCode, String language, String userId) {
        try {
            log.info("REST createVnPayPayment - amount={}, userId={}", amount, userId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/internal/payments/create")
                    .body(Map.of(
                            "amount", amount,
                            "bankCode", bankCode != null ? bankCode : "NCB",
                            "language", language != null ? language : "vn",
                            "userId", userId != null ? userId : ""
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new BadRequestException("Empty response from order-service");
            }
            return new PaymentCreateResult(
                    (String) response.get("code"),
                    (String) response.get("message"),
                    (String) response.get("paymentUrl"),
                    (String) response.get("transactionId"),
                    null
            );
        } catch (RestClientException e) {
            log.error("Failed to create VNPay payment via REST", e);
            throw new BadRequestException("Failed to create payment: " + e.getMessage());
        }
    }

    public PaymentCreateResult createVnPayPayment(Long amount, String bankCode, String language, String userId, String returnUrl) {
        try {
            log.info("REST createVnPayPayment - amount={}, userId={}, returnUrl={}", amount, userId, returnUrl);
            
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("amount", amount);
            requestBody.put("bankCode", bankCode != null ? bankCode : "NCB");
            requestBody.put("language", language != null ? language : "vn");
            requestBody.put("userId", userId != null ? userId : "");
            if (returnUrl != null && !returnUrl.trim().isEmpty()) {
                requestBody.put("returnUrl", returnUrl.trim());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/internal/payments/create")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new BadRequestException("Empty response from order-service");
            }
            return new PaymentCreateResult(
                    (String) response.get("code"),
                    (String) response.get("message"),
                    (String) response.get("paymentUrl"),
                    (String) response.get("transactionId"),
                    returnUrl
            );
        } catch (RestClientException e) {
            log.error("Failed to create VNPay payment via REST", e);
            throw new BadRequestException("Failed to create payment: " + e.getMessage());
        }
    }

    public PaymentVerifyResult verifyPaymentCallback(String transactionId, String orderId,
            String responseCode, String secureHash) {
        try {
            log.info("REST verifyPaymentCallback - transactionId={}", transactionId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/internal/payments/verify")
                    .body(Map.of(
                            "transactionId", transactionId != null ? transactionId : "",
                            "orderId", orderId != null ? orderId : "",
                            "responseCode", responseCode != null ? responseCode : "",
                            "secureHash", secureHash != null ? secureHash : ""
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return new PaymentVerifyResult(false, "ERROR", "Empty response");
            }
            boolean valid = Boolean.TRUE.equals(response.get("valid"));
            return new PaymentVerifyResult(valid,
                    (String) response.getOrDefault("status", "UNKNOWN"),
                    (String) response.getOrDefault("message", ""));
        } catch (RestClientException e) {
            log.error("Failed to verify payment callback via REST", e);
            throw new BadRequestException("Failed to verify payment: " + e.getMessage());
        }
    }

    public PaymentStatusResult getPaymentStatus(String transactionId) {
        try {
            log.info("REST getPaymentStatus - transactionId={}", transactionId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/api/internal/payments/{transactionId}/status", transactionId)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return new PaymentStatusResult("", transactionId, "NOT_FOUND", 0L, "", "");
            }
            return new PaymentStatusResult(
                    (String) response.getOrDefault("paymentId", ""),
                    (String) response.getOrDefault("transactionId", transactionId),
                    (String) response.getOrDefault("status", "UNKNOWN"),
                    response.get("amount") instanceof Number n ? n.longValue() : 0L,
                    (String) response.getOrDefault("method", ""),
                    (String) response.getOrDefault("paidAt", "")
            );
        } catch (RestClientException e) {
            log.error("Failed to get payment status via REST", e);
            throw new BadRequestException("Failed to get payment status: " + e.getMessage());
        }
    }

    public void updatePaymentStatus(String transactionId, String status) {
        try {
            log.info("REST updatePaymentStatus - transactionId={}, status={}", transactionId, status);
            restClient.patch()
                    .uri("/api/internal/payments/{transactionId}/status", transactionId)
                    .body(Map.of("status", status))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Failed to update payment status via REST", e);
            throw new BadRequestException("Failed to update payment status: " + e.getMessage());
        }
    }

    public record PaymentCreateResult(String code, String message, String paymentUrl, String transactionId, String returnUrl) {}
    public record PaymentVerifyResult(boolean valid, String status, String message) {}
    public record PaymentStatusResult(String paymentId, String transactionId, String status,
                                      long amount, String method, String paidAt) {}
}
