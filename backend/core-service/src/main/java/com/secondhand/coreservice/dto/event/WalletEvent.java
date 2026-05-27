package com.secondhand.coreservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Wallet Event DTO nhận từ RabbitMQ (published bởi Order Service).
 * Chứa thông tin escrow release/refund cần xử lý.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;   // ESCROW_RELEASE, ESCROW_REFUND
    private String userId;
    private double amount;
    private String orderId;
    private LocalDateTime timestamp;
}
