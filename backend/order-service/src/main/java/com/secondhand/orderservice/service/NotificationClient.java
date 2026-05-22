package com.secondhand.orderservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.core-service.url:http://ktpm-core-service:8082}")
    private String coreServiceUrl;

    public void sendNotification(String userId, String content, String type, String itemId) {
        try {
            String url = coreServiceUrl + "/api/notifications/internal";
            log.info("Sending notification request to: {}, userId: {}, type: {}", url, userId, type);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("userId", userId);
            request.put("content", content);
            request.put("type", type);
            request.put("itemId", itemId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("Notification successfully sent to core-service");
        } catch (Exception e) {
            log.error("Failed to send notification to core-service: {}", e.getMessage(), e);
        }
    }
}
