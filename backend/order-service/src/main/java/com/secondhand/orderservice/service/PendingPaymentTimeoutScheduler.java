package com.secondhand.orderservice.service;

import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderItem;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import com.secondhand.orderservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự động hủy các đơn VNPay chưa thanh toán sau 2 phút.
 *
 * Mục tiêu:
 * - tránh đơn bị treo ở PENDING_PAYMENT quá lâu
 * - trả item về ACTIVE để người mua khác có thể mua lại
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentTimeoutScheduler {

    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ItemClient itemClient;
    private final NotificationClient notificationClient;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredPendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Order> expiredOrders = orderRepository.findPendingPaymentOrdersBefore(cutoff);

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (Order order : expiredOrders) {
            try {
                if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                        || order.getPaymentStatus() != PaymentStatus.PENDING) {
                    continue;
                }

                LocalDateTime now = LocalDateTime.now();
                order.setStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setCancelReason("Thanh toan VNPay qua han sau 15 phut");
                order.setUpdatedAt(now);

                Payment payment = order.getPayment();
                if (payment != null) {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                }

                orderRepository.save(order);

                for (OrderItem item : order.getOrderItems()) {
                    try {
                        itemClient.updateItemStatus(item.getItemId(), "ACTIVE");
                    } catch (Exception itemError) {
                        log.error("[PendingPaymentTimeout] Failed to release item {} for order {}: {}",
                                item.getItemId(), order.getId(), itemError.getMessage(), itemError);
                    }
                }

                notificationClient.sendNotification(
                        order.getBuyerId(),
                        "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() +
                                " đã bị hủy do không thanh toán trong 2 phút.",
                        "ORDER_STATUS",
                        order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getItemId()
                );

                log.info("[PendingPaymentTimeout] Cancelled expired order {}", order.getId());
            } catch (Exception e) {
                log.error("[PendingPaymentTimeout] Failed to cancel expired order {}: {}",
                        order.getId(), e.getMessage(), e);
            }
        }
    }
}