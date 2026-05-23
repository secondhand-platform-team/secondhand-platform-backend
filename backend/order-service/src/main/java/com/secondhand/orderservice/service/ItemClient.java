package com.secondhand.orderservice.service;

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
public class ItemClient {

    private final RestTemplate restTemplate = new RestTemplate();

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
}
