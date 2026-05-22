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
public class WalletClient {
 
    private final RestTemplate restTemplate = new RestTemplate();
 
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
            // Don't let seller refund fail the whole order flow if it's secondary, 
            // but for safety we rethrow or just log.
            throw new RuntimeException("Không thể cộng tiền vào ví của người bán: " + e.getMessage());
        }
    }
}
