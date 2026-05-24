package com.secondhand.orderservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event DTO gửi qua RabbitMQ cho notification async.
 * 
 * Thay vì Order Service gọi REST → Core Service (sync, tight coupling),
 * giờ publish event → RabbitMQ → Core Service consume async.
 * 
 * Lợi ích:
 * - Order API trả về nhanh hơn
 * - Không phụ thuộc Core Service availability
 * - Retry tự động nếu consumer lỗi
 * - Scale notification consumer độc lập
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Loại event: ORDER_CREATED, ORDER_CANCELLED, ORDER_COMPLETED, ... */
    private String eventType;

    /** ID user nhận notification */
    private String userId;

    /** Nội dung thông báo */
    private String content;

    /** Loại notification (map sang NotificationType enum ở Core Service) */
    private String notificationType;

    /** ID item liên quan (nullable) */
    private String itemId;

    /** Timestamp tạo event */
    private LocalDateTime timestamp;
}
