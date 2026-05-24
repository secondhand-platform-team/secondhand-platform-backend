package com.secondhand.coreservice.consumer;

import com.secondhand.coreservice.dto.event.NotificationEvent;
import com.secondhand.coreservice.model.enums.NotificationType;
import com.secondhand.coreservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ Consumer — Notification Events
 * 
 * Nhận event từ Order Service (async) → tạo notification + push WebSocket.
 * 
 * Lợi ích so với REST sync trước đây:
 * - Order API không phải chờ notification tạo xong
 * - Retry tự động nếu lỗi (RabbitMQ requeue)
 * - Scale consumer độc lập
 * - Fault isolation: Core Service down → message queue lại, không mất
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.queue")
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            log.info("Received notification event: type={}, userId={}", 
                    event.getEventType(), event.getUserId());

            // Map string sang NotificationType enum
            NotificationType type;
            try {
                type = NotificationType.valueOf(event.getNotificationType());
            } catch (IllegalArgumentException e) {
                // Fallback: nếu type không match enum, dùng SYSTEM
                log.warn("Unknown notification type: {}, fallback to SYSTEM", event.getNotificationType());
                type = NotificationType.SYSTEM;
            }

            // Tạo notification + WebSocket push (logic sẵn có)
            notificationService.createAndSendNotification(
                    event.getUserId(),
                    event.getContent(),
                    type,
                    event.getItemId()
            );

            log.info("Notification created successfully for user={}, type={}", 
                    event.getUserId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to process notification event: type={}, userId={}, error={}",
                    event.getEventType(), event.getUserId(), e.getMessage(), e);
            // Không throw → message sẽ bị ack, tránh infinite retry loop
            // Trong production có thể gửi sang Dead Letter Queue
        }
    }
}
