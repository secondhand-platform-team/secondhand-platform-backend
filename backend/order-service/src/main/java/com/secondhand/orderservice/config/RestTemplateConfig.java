package com.secondhand.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration — Connection Pooling
 * 
 * Trước đây mỗi Client (WalletClient, ItemClient, NotificationClient) tự tạo
 * `new RestTemplate()` → không có connection pool, lãng phí resource.
 * 
 * Giờ inject shared Bean với timeout config.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5s connect timeout
        factory.setReadTimeout(10000);     // 10s read timeout
        return new RestTemplate(factory);
    }
}
