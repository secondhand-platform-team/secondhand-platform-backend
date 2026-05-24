package com.secondhand.orderservice.model;

import com.secondhand.orderservice.model.enums.OrderEventType;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Event Sourcing — OrderEvent Entity
 * 
 * Mỗi lần order thay đổi trạng thái → 1 event được append vào bảng order_events.
 * Events là IMMUTABLE (chỉ INSERT, không UPDATE/DELETE).
 * 
 * Lợi ích:
 * - Audit trail: biết chính xác ai, khi nào, làm gì
 * - Replay: có thể rebuild lại trạng thái order từ events
 * - Debug: dễ trace issue trong production
 * - Compliance: đáp ứng yêu cầu audit cho giao dịch tài chính
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "order_events", indexes = {
    @Index(name = "idx_order_events_order_id", columnList = "orderId"),
    @Index(name = "idx_order_events_created_at", columnList = "createdAt")
})
public class OrderEvent {

    @Id
    private String id;

    /** ID đơn hàng liên quan */
    private String orderId;

    /** Loại event (immutable fact) */
    @Enumerated(EnumType.STRING)
    private OrderEventType eventType;

    /** User thực hiện hành động */
    private String actorId;

    /** Vai trò: BUYER, SELLER, SYSTEM, ADMIN */
    private String actorRole;

    /** Metadata dạng JSON: reason, amount, oldStatus, newStatus... */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /** Snapshot trạng thái order tại thời điểm event (JSON) */
    @Column(columnDefinition = "TEXT")
    private String snapshotJson;

    /** Timestamp event xảy ra (immutable) */
    private LocalDateTime createdAt;
}
