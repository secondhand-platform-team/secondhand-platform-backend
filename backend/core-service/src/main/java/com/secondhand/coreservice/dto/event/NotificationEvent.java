package com.secondhand.coreservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event DTO nhận từ RabbitMQ (published bởi Order Service).
 * Chứa thông tin notification cần tạo và push qua WebSocket.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;
    private String userId;
    private String content;
    private String notificationType;
    private String itemId;
    private LocalDateTime timestamp;
}
