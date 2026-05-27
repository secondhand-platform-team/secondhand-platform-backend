package com.secondhand.orderservice.service;

import com.secondhand.orderservice.config.RabbitMQConfig;
import com.secondhand.orderservice.dto.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Notification Client — Event-driven via RabbitMQ
 * 
 * TRƯỚC: Gọi REST sync → Core Service → tạo notification
 *   - Vấn đề: Core down → Order fail, API chậm, tight coupling
 * 
 * SAU: Publish event → RabbitMQ → Core Service consume async
 *   - Order API nhanh hơn
 *   - Không phụ thuộc Core Service availability
 *   - RabbitMQ tự retry nếu consumer lỗi
 *   - Scale notification consumer độc lập
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationClient {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish notification event lên RabbitMQ (async, fire-and-forget)
     * Core Service sẽ consume event này và tạo notification + WebSocket push
     */
    public void sendNotification(String userId, String content, String type, String itemId) {
        try {
            NotificationEvent event = NotificationEvent.builder()
                    .eventType(type)
                    .userId(userId)
                    .content(content)
                    .notificationType(type)
                    .itemId(itemId)
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    "notification." + type.toLowerCase(),
                    event
            );

            log.info("Published notification event: type={}, userId={}", type, userId);
        } catch (Exception e) {
            // Log lỗi nhưng KHÔNG throw → order không bị fail vì notification
            log.error("Failed to publish notification event: type={}, userId={}, error={}",
                    type, userId, e.getMessage());
        }
    }
}
