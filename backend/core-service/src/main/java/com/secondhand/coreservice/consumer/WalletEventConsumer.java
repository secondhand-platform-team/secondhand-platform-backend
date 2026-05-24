package com.secondhand.coreservice.consumer;

import com.secondhand.coreservice.dto.event.WalletEvent;
import com.secondhand.coreservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ Consumer — Wallet Events (Escrow Release/Refund)
 * 
 * Nhận event từ Order Service (async) → xử lý escrow.
 * 
 * Tại sao async?
 * - escrowRelease: Order đã COMPLETED, tiền cho seller không cần ngay lập tức
 * - escrowRefund: Order đã CANCELLED, tiền hoàn buyer trong vài giây
 * - Tránh blocking Order API khi Core Service chậm
 * - RabbitMQ tự retry nếu xử lý lỗi
 * 
 * Lưu ý: escrowHold vẫn sync (REST) vì cần biết ngay buyer đủ tiền không.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletEventConsumer {

    private final WalletService walletService;

    @RabbitListener(queues = "wallet.queue")
    public void handleWalletEvent(WalletEvent event) {
        try {
            log.info("Received wallet event: type={}, userId={}, amount={}, orderId={}",
                    event.getEventType(), event.getUserId(), event.getAmount(), event.getOrderId());

            switch (event.getEventType()) {
                case "ESCROW_RELEASE" -> {
                    walletService.escrowRelease(event.getUserId(), event.getAmount(), event.getOrderId());
                    log.info("Escrow RELEASE processed: userId={}, amount={}", event.getUserId(), event.getAmount());
                }
                case "ESCROW_REFUND" -> {
                    walletService.escrowRefund(event.getUserId(), event.getAmount(), event.getOrderId());
                    log.info("Escrow REFUND processed: userId={}, amount={}", event.getUserId(), event.getAmount());
                }
                default -> log.warn("Unknown wallet event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process wallet event: type={}, userId={}, error={}",
                    event.getEventType(), event.getUserId(), e.getMessage(), e);
            // Không throw → message sẽ bị ack
            // Production: gửi sang Dead Letter Queue để xử lý lại
        }
    }
}
