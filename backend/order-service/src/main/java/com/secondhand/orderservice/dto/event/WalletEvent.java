package com.secondhand.orderservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Wallet Event DTO — gửi qua RabbitMQ cho escrow release/refund async.
 * 
 * escrowHold vẫn sync (cần biết đủ tiền không).
 * escrowRelease và escrowRefund → async (order đã hoàn tất, tiền vào ví sau vài giây).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Loại event: ESCROW_RELEASE, ESCROW_REFUND */
    private String eventType;

    /** ID user nhận tiền */
    private String userId;

    /** Số tiền */
    private double amount;

    /** ID đơn hàng liên quan */
    private String orderId;

    /** Timestamp */
    private LocalDateTime timestamp;
}
