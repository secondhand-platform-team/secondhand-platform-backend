package com.secondhand.authservice.client;

import com.secondhand.authservice.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
@Slf4j
public class CartRestClient {

    private final RestClient restClient;

    public CartRestClient(@Value("${order.service.url}") String orderServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    public void createCart(String userId) {
        try {
            restClient.post()
                    .uri("/api/internal/carts")
                    .body(Map.of("userId", userId))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Cart created successfully for userId={}", userId);
        } catch (RestClientException e) {
            log.error("Failed to create cart for userId={}", userId, e);
            throw new BadRequestException("Cannot create cart for user");
        }
    }
}
