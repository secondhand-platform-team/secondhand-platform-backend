package com.secondhand.orderservice.service;
 
import com.secondhand.orderservice.config.RabbitMQConfig;
import com.secondhand.orderservice.dto.event.WalletEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
 
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
 
/**
 * Wallet Client — Hybrid Sync/Async
 * 
 * - escrowHold(): Sync REST — cần biết ngay buyer đủ tiền không
 * - escrowRelease(): Async RabbitMQ — order đã hoàn tất, tiền vào ví sau vài giây
 * - escrowRefund(): Async RabbitMQ — hủy đơn, tiền hoàn sau vài giây
 * 
 * Lý do tách sync/async:
 * - Hold PHẢI sync: nếu không đủ tiền → fail ngay, không tạo order
 * - Release/Refund CÓ THỂ async: order đã tồn tại, tiền sẽ vào ví đúng lúc
 * - Async tránh blocking API khi Core Service chậm/down
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletClient {
 
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
 
    @Value("${app.core-service.url:http://ktpm-core-service:8082}")
    private String coreServiceUrl;
 
    public void deduct(String userId, double amount, String description) {
        try {
            String url = coreServiceUrl + "/api/wallet/internal/deduct";
            log.info("Sending wallet deduct request to: {}, userId: {}, amount: {}", url, userId, amount);
 
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
 
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("amount", amount);
            request.put("description", description);
 
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Wallet deducted successfully via core-service");
        } catch (Exception e) {
            log.error("Failed to deduct wallet via core-service: {}", e.getMessage(), e);
            throw new RuntimeException("Giao dịch thanh toán ví thất bại: Số dư ví không đủ hoặc xảy ra lỗi.");
        }
    }
 
    public void add(String userId, double amount, String description) {
        try {
            String url = coreServiceUrl + "/api/wallet/internal/add";
            log.info("Sending wallet add request to: {}, userId: {}, amount: {}", url, userId, amount);
 
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
 
            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("amount", amount);
            request.put("description", description);
 
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Wallet added successfully via core-service");
        } catch (Exception e) {
            log.error("Failed to add money to wallet via core-service: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể cộng tiền vào ví của người bán: " + e.getMessage());
        }
    }

    // ====== Escrow Methods ======

    /**
     * Escrow Hold — SYNC (REST)
     * PHẢI sync vì cần biết ngay: buyer đủ tiền hay không?
     * Nếu không đủ → fail → không tạo order.
     */
    public void escrowHold(String userId, double amount, String orderId) {
        try {
            String url = coreServiceUrl + "/api/wallet/internal/escrow-hold";
            log.info("Escrow HOLD (sync): userId={}, amount={}, orderId={}", userId, amount, orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("amount", amount);
            request.put("orderId", orderId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Escrow HOLD successful");
        } catch (Exception e) {
            log.error("Escrow HOLD failed: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạm giữ tiền: Số dư ví không đủ hoặc xảy ra lỗi.");
        }
    }

    /**
     * Escrow Release — ASYNC (RabbitMQ)
     * Order đã hoàn tất → tiền seller sẽ vào ví sau vài giây.
     * Không cần blocking API cho operation này.
     */
    public void escrowRelease(String userId, double amount, String orderId) {
        try {
            WalletEvent event = WalletEvent.builder()
                    .eventType("ESCROW_RELEASE")
                    .userId(userId)
                    .amount(amount)
                    .orderId(orderId)
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WALLET_EXCHANGE,
                    "wallet.escrow.release",
                    event
            );
            log.info("Escrow RELEASE published (async): userId={}, amount={}, orderId={}", userId, amount, orderId);
        } catch (Exception e) {
            log.error("Failed to publish escrow RELEASE event: {}", e.getMessage(), e);
            // Fallback: gọi REST sync nếu RabbitMQ lỗi
            escrowReleaseFallback(userId, amount, orderId);
        }
    }

    /**
     * Escrow Refund — ASYNC (RabbitMQ)
     * Đơn bị hủy → tiền buyer sẽ hoàn lại sau vài giây.
     */
    public void escrowRefund(String userId, double amount, String orderId) {
        try {
            WalletEvent event = WalletEvent.builder()
                    .eventType("ESCROW_REFUND")
                    .userId(userId)
                    .amount(amount)
                    .orderId(orderId)
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WALLET_EXCHANGE,
                    "wallet.escrow.refund",
                    event
            );
            log.info("Escrow REFUND published (async): userId={}, amount={}, orderId={}", userId, amount, orderId);
        } catch (Exception e) {
            log.error("Failed to publish escrow REFUND event: {}", e.getMessage(), e);
            // Fallback: gọi REST sync nếu RabbitMQ lỗi
            escrowRefundFallback(userId, amount, orderId);
        }
    }

    // ====== Fallback (REST sync) ======

    private void escrowReleaseFallback(String userId, double amount, String orderId) {
        try {
            String url = coreServiceUrl + "/api/wallet/internal/escrow-release";
            log.warn("Escrow RELEASE fallback to REST sync: userId={}, orderId={}", userId, orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("amount", amount);
            request.put("orderId", orderId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Escrow RELEASE fallback successful");
        } catch (Exception e2) {
            log.error("Escrow RELEASE fallback ALSO failed: {}", e2.getMessage(), e2);
        }
    }

    private void escrowRefundFallback(String userId, double amount, String orderId) {
        try {
            String url = coreServiceUrl + "/api/wallet/internal/escrow-refund";
            log.warn("Escrow REFUND fallback to REST sync: userId={}, orderId={}", userId, orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("amount", amount);
            request.put("orderId", orderId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Escrow REFUND fallback successful");
        } catch (Exception e2) {
            log.error("Escrow REFUND fallback ALSO failed: {}", e2.getMessage(), e2);
        }
    }
}
