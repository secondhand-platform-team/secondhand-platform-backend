package com.secondhand.orderservice.service;

import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Auto-complete đơn hàng DELIVERED sau 3 ngày nếu buyer không xác nhận.
 * → COMPLETED + escrow release + item → SOLD + notify cả 2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCompleteScheduler {

    private final OrderRepository orderRepository;
    private final WalletClient walletClient;
    private final ItemClient itemClient;
    private final NotificationClient notificationClient;

    @Scheduled(fixedRate = 60000) // check mỗi 1 phút
    @Transactional
    public void autoCompleteOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findOrdersToAutoComplete(now);

        for (Order order : orders) {
            try {
                log.info("[AutoComplete] Processing order {}", order.getId());

                order.setStatus(OrderStatus.COMPLETED);
                order.setUpdatedAt(now);

                // Escrow release → seller
                walletClient.escrowRelease(order.getSellerId(), order.getTotalPrice(), order.getId());

                // Item → SOLD
                String itemId = order.getOrderItems().isEmpty() ? null
                        : order.getOrderItems().get(0).getItemId();
                if (itemId != null) {
                    itemClient.updateItemStatus(itemId, "SOLD");
                }

                orderRepository.save(order);

                // Notify buyer
                notificationClient.sendNotification(
                        order.getBuyerId(),
                        "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() +
                                " đã tự động hoàn tất sau 3 ngày.",
                        "ORDER_AUTO_COMPLETED",
                        itemId
                );

                // Notify seller
                notificationClient.sendNotification(
                        order.getSellerId(),
                        "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() +
                                " đã tự động hoàn tất. " + (long) order.getTotalPrice().doubleValue() +
                                " VNĐ đã chuyển vào ví.",
                        "ORDER_AUTO_COMPLETED",
                        itemId
                );

                log.info("[AutoComplete] Order {} auto-completed, escrow released to seller {}", order.getId(), order.getSellerId());
            } catch (Exception e) {
                log.error("[AutoComplete] Failed to auto-complete order {}: {}", order.getId(), e.getMessage(), e);
            }
        }
    }
}
