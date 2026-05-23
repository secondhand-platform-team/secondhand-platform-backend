package com.secondhand.orderservice.service;

import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.Shipment;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.model.enums.ShipmentStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Giả lập bên giao hàng (shipper).
 * - HANDOVER_TO_SHIPPER → (30s) → IN_TRANSIT
 * - IN_TRANSIT → (60s) → DELIVERED + set autoCompleteAt
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentSimulatorService {

    private final OrderRepository orderRepository;
    private final NotificationClient notificationClient;

    @Scheduled(fixedRate = 15000) // check mỗi 15 giây
    @Transactional
    public void simulateShipment() {
        // 1. HANDOVER_TO_SHIPPER → IN_TRANSIT (sau 30s)
        LocalDateTime threshold1 = LocalDateTime.now().minusSeconds(30);
        List<Order> handoverOrders = orderRepository.findByStatusAndUpdatedAtBefore(
                OrderStatus.HANDOVER_TO_SHIPPER, threshold1);

        for (Order order : handoverOrders) {
            try {
                order.setStatus(OrderStatus.IN_TRANSIT);
                order.setUpdatedAt(LocalDateTime.now());

                if (order.getShipment() != null) {
                    order.getShipment().setStatus(ShipmentStatus.IN_TRANSIT);
                    order.getShipment().setCurrentLocation("Đang vận chuyển từ kho phân loại");
                }

                orderRepository.save(order);

                String itemId = order.getOrderItems().isEmpty() ? null
                        : order.getOrderItems().get(0).getItemId();

                // Notify buyer
                notificationClient.sendNotification(
                        order.getBuyerId(),
                        "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đang được giao đến bạn.",
                        "ORDER_IN_TRANSIT",
                        itemId
                );

                // Notify seller
                notificationClient.sendNotification(
                        order.getSellerId(),
                        "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đang trên đường giao đến người mua.",
                        "ORDER_IN_TRANSIT",
                        itemId
                );

                log.info("[ShipperSim] Order {} → IN_TRANSIT", order.getId());
            } catch (Exception e) {
                log.error("[ShipperSim] Failed to transition order {} to IN_TRANSIT: {}", order.getId(), e.getMessage());
            }
        }

        // 2. IN_TRANSIT → DELIVERED (sau 60s)
        LocalDateTime threshold2 = LocalDateTime.now().minusSeconds(60);
        List<Order> transitOrders = orderRepository.findByStatusAndUpdatedAtBefore(
                OrderStatus.IN_TRANSIT, threshold2);

        for (Order order : transitOrders) {
            try {
                order.setStatus(OrderStatus.DELIVERED);
                order.setAutoCompleteAt(LocalDateTime.now().plusDays(3)); // auto-complete sau 3 ngày
                order.setUpdatedAt(LocalDateTime.now());

                if (order.getShipment() != null) {
                    order.getShipment().setStatus(ShipmentStatus.DELIVERED);
                    order.getShipment().setDeliveredAt(LocalDateTime.now());
                    order.getShipment().setCurrentLocation("Đã giao tại địa chỉ người nhận");
                }

                orderRepository.save(order);

                String itemId = order.getOrderItems().isEmpty() ? null
                        : order.getOrderItems().get(0).getItemId();

                // Notify buyer
                notificationClient.sendNotification(
                        order.getBuyerId(),
                        "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() +
                                " đã giao thành công! Vui lòng xác nhận nhận hàng trong 3 ngày.",
                        "ORDER_DELIVERED",
                        itemId
                );

                // Notify seller
                notificationClient.sendNotification(
                        order.getSellerId(),
                        "Shipper đã giao hàng đơn #" + order.getId().substring(0, 8).toUpperCase() +
                                ". Chờ người mua xác nhận nhận hàng.",
                        "ORDER_DELIVERED",
                        itemId
                );

                log.info("[ShipperSim] Order {} → DELIVERED, autoCompleteAt={}", order.getId(), order.getAutoCompleteAt());
            } catch (Exception e) {
                log.error("[ShipperSim] Failed to transition order {} to DELIVERED: {}", order.getId(), e.getMessage());
            }
        }
    }
}
