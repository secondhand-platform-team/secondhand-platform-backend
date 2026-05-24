package com.secondhand.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * REST client gọi core-service để quản lý item status và lấy thông tin item.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItemClient {

    private final RestTemplate restTemplate;

    @Value("${app.core-service.url:http://ktpm-core-service:8082}")
    private String coreServiceUrl;

    /**
     * Cập nhật status item (RESERVED, SOLD, ACTIVE, ...)
     */
    public void updateItemStatus(String itemId, String status) {
        try {
            String url = coreServiceUrl + "/api/items/internal/" + itemId + "/status";
            log.info("Updating item status: itemId={}, status={}", itemId, status);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("status", status);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            log.info("Item {} status updated to {} successfully", itemId, status);
        } catch (Exception e) {
            log.error("Failed to update item status: itemId={}, status={}, error={}", itemId, status, e.getMessage(), e);
            throw new RuntimeException("Không thể cập nhật trạng thái sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin item từ core-service (không cần auth)
     * Trả về Map chứa thông tin item (itemId, title, price, status, userId, images...)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getItem(String itemId) {
        try {
            String url = coreServiceUrl + "/api/items/internal/" + itemId;
            log.info("Getting item info: itemId={}", itemId);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            log.info("Got item info for {}", itemId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get item info: itemId={}, error={}", itemId, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy thông tin sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Reserve item (atomic, dùng SELECT FOR UPDATE ở Core Service)
     * Đảm bảo chỉ 1 buyer có thể reserve item tại một thời điểm.
     * Giải quyết Race Condition khi 2 buyer mua cùng lúc.
     *
     * @return Map chứa thông tin item đã reserved
     * @throws RuntimeException nếu item không ACTIVE (đã bị người khác mua)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> reserveItem(String itemId, String buyerId) {
        try {
            String url = coreServiceUrl + "/api/items/internal/" + itemId + "/reserve";
            log.info("Reserving item: itemId={}, buyerId={}", itemId, buyerId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("buyerId", buyerId);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            log.info("Item {} reserved successfully for buyer {}", itemId, buyerId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to reserve item: itemId={}, buyerId={}, error={}", itemId, buyerId, e.getMessage(), e);
            throw new RuntimeException("Sản phẩm không còn khả dụng hoặc đã được người khác đặt mua.");
        }
    }
}
