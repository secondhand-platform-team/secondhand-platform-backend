package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.model.*;
import com.secondhand.orderservice.model.enums.*;
import com.secondhand.orderservice.repository.OrderRepository;
import com.secondhand.orderservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CQRS — Command Service Implementation
 * * Chứa tất cả Write operations (tạo, sửa, hủy, chuyển trạng thái).
 * Sau mỗi state change → ghi OrderEvent (Event Sourcing).
 * * Pattern áp dụng:
 * - CQRS: Tách riêng Command khỏi Query
 * - Event Sourcing: Ghi lại mọi thay đổi thành immutable events
 * - Observer: Publish notification/wallet events qua RabbitMQ
 */
@Service("orderCommandService")
@RequiredArgsConstructor
@Slf4j
public class OrderCommandServiceImpl implements OrderCommandService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final WalletClient walletClient;
    private final ItemClient itemClient;
    private final NotificationClient notificationClient;
    private final PaymentService paymentService;
    private final com.secondhand.orderservice.repository.PaymentRepository paymentRepository;
    private final OrderEventStore eventStore;

    // ====================================================================
    // BUYER: Tạo đơn hàng
    // ====================================================================

    @Override
    @Transactional
    public Order createOrder(String buyerId, CreateOrderRequest request) {
        log.info("Creating order for buyer={}, itemId={}", buyerId, request.getItemId());

        boolean itemReserved = false;

        try {
            Map<String, Object> itemData = itemClient.reserveItem(request.getItemId(), buyerId);
            itemReserved = true;
            String sellerId = (String) itemData.get("userId");
            String itemName = (String) itemData.get("title");
            Object priceObj = itemData.get("price");
            double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : Double.parseDouble(priceObj.toString());

            String itemImageUrl = null;
            Object imagesObj = itemData.get("images");
            if (imagesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> images = (List<Map<String, Object>>) imagesObj;
                if (!images.isEmpty()) {
                    itemImageUrl = (String) images.get(0).get("url");
                }
            }

            if (buyerId.equals(sellerId)) {
                throw new RuntimeException("Bạn không thể mua sản phẩm của chính mình.");
            }

            String orderId = UUID.randomUUID().toString();

            Order order = new Order();
            order.setId(orderId);
            order.setBuyerId(buyerId);
            order.setSellerId(sellerId);
            order.setTotalPrice(price);

            boolean isVnPay = "VNPAY".equalsIgnoreCase(request.getPaymentMethod());

            if (isVnPay) {
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                order.setPaymentStatus(PaymentStatus.PENDING);
            } else {
                order.setStatus(OrderStatus.PAID);
                order.setPaymentStatus(PaymentStatus.PAID);
                walletClient.escrowHold(buyerId, price, orderId);
                order.setEscrowTransactionId("ESCROW-HOLD-" + orderId);
            }

            order.setReceiverName(request.getReceiverName());
            order.setReceiverPhone(request.getReceiverPhone());
            order.setShippingAddress(request.getShippingAddress());
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());

            OrderItem orderItem = new OrderItem();
            orderItem.setId(UUID.randomUUID().toString());
            orderItem.setItemId(request.getItemId());
            orderItem.setItemName(itemName);
            orderItem.setSellerId(sellerId);
            orderItem.setPrice(price);
            orderItem.setItemImageUrl(itemImageUrl);
            orderItem.setOrder(order);
            order.setOrderItems(List.of(orderItem));

            Order savedOrder = orderRepository.save(order);

            if (isVnPay) {
                com.secondhand.orderservice.dto.response.PaymentResponse payRes =
                    paymentService.createVnPayPaymentInternal((long) price, null, "vn", buyerId, null, savedOrder.getId());
                savedOrder.setPaymentUrl(payRes.getPaymentUrl());

                log.info("VNPay order {} created with PENDING_PAYMENT status for buyer={}", orderId, buyerId);
                return savedOrder;
            }

            try {
                cartService.removeItemFromCart(buyerId, request.getItemId());
            } catch (Exception e) {
                log.warn("Failed to remove item from cart: {}", e.getMessage());
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("itemId", request.getItemId());
            metadata.put("itemName", itemName);
            metadata.put("price", price);
            metadata.put("paymentMethod", request.getPaymentMethod());
            eventStore.recordEvent(orderId, OrderEventType.ORDER_CREATED, buyerId, "BUYER", metadata, savedOrder);

            if (!isVnPay) {
                Map<String, Object> escrowMeta = new LinkedHashMap<>();
                escrowMeta.put("amount", price);
                escrowMeta.put("type", "HOLD");
                eventStore.recordEvent(orderId, OrderEventType.ESCROW_HELD, buyerId, "SYSTEM", escrowMeta, savedOrder);
            }

            notificationClient.sendNotification(
                    buyerId,
                    "Đặt hàng thành công! Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đang chờ người bán xử lý.",
                    "ORDER_CREATED",
                    request.getItemId()
            );

            notificationClient.sendNotification(
                    sellerId,
                    "Sản phẩm \"" + itemName + "\" đã được đặt mua! Vui lòng chuẩn bị hàng.",
                    "ORDER_NEW_FOR_SELLER",
                    request.getItemId()
            );

            log.info("Order {} created successfully, escrow held for buyer={}", orderId, buyerId);
            return savedOrder;
        } catch (RuntimeException e) {
            releaseReservedItem(request.getItemId(), itemReserved, e);
            throw e;
        } catch (Exception e) {
            releaseReservedItem(request.getItemId(), itemReserved, e);
            throw new RuntimeException("Không thể tạo đơn hàng: " + e.getMessage(), e);
        }
    }

    // ====================================================================
    // BUYER: Hủy đơn
    // ====================================================================

    @Override
    @Transactional
    public Order cancelOrder(String orderId, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi đang ở trạng thái 'Đã thanh toán' hoặc 'Chờ thanh toán'.");
        }

        String oldStatus = order.getStatus().name();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Người mua hủy đơn");
        order.setUpdatedAt(LocalDateTime.now());

        if (oldStatus.equals(OrderStatus.PAID.name())) {
            walletClient.escrowRefund(buyerId, order.getTotalPrice(), orderId);
        }

        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "ACTIVE");

        Order saved = orderRepository.save(order);

        // Event Sourcing
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("oldStatus", oldStatus);
        metadata.put("reason", "Người mua hủy đơn");
        eventStore.recordEvent(orderId, OrderEventType.ORDER_CANCELLED, buyerId, "BUYER", metadata, saved);

        if (oldStatus.equals(OrderStatus.PAID.name())) {
            Map<String, Object> refundMeta = new LinkedHashMap<>();
            refundMeta.put("amount", order.getTotalPrice());
            refundMeta.put("type", "REFUND");
            eventStore.recordEvent(orderId, OrderEventType.ESCROW_REFUNDED, buyerId, "SYSTEM", refundMeta, saved);
        }

        // Notify
        notificationClient.sendNotification(order.getSellerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã bị người mua hủy.",
                "ORDER_CANCELLED", itemId);
        notificationClient.sendNotification(buyerId,
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã hủy thành công. Tiền đã hoàn vào ví.",
                "ORDER_CANCELLED", itemId);

        log.info("Order {} cancelled by buyer {}", orderId, buyerId);
        return saved;
    }

    // ====================================================================
    // BUYER: Xác nhận nhận hàng
    // ====================================================================

    @Override
    @Transactional
    public Order confirmReceived(String orderId, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Chỉ có thể xác nhận nhận hàng khi đơn đã giao.");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());

        walletClient.escrowRelease(order.getSellerId(), order.getTotalPrice(), orderId);

        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "SOLD");

        Order saved = orderRepository.save(order);

        // Event Sourcing
        eventStore.recordEvent(orderId, OrderEventType.ORDER_COMPLETED, buyerId, "BUYER", null, saved);

        Map<String, Object> releaseMeta = new LinkedHashMap<>();
        releaseMeta.put("amount", order.getTotalPrice());
        releaseMeta.put("sellerId", order.getSellerId());
        releaseMeta.put("type", "RELEASE");
        eventStore.recordEvent(orderId, OrderEventType.ESCROW_RELEASED, order.getSellerId(), "SYSTEM", releaseMeta, saved);

        // Notify
        notificationClient.sendNotification(order.getSellerId(),
                "Người mua đã xác nhận nhận hàng! " + (long) order.getTotalPrice().doubleValue() + " VNĐ đã chuyển vào ví của bạn.",
                "ORDER_COMPLETED", itemId);
        notificationClient.sendNotification(buyerId,
                "Cảm ơn bạn! Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã hoàn tất.",
                "ORDER_COMPLETED", itemId);

        log.info("Order {} confirmed received by buyer {}", orderId, buyerId);
        return saved;
    }

    // ====================================================================
    // BUYER: Khiếu nại
    // ====================================================================

    @Override
    @Transactional
    public Order disputeOrder(String orderId, String buyerId, String reason) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Chỉ có thể khiếu nại khi đơn đã giao.");
        }

        order.setStatus(OrderStatus.DISPUTED);
        order.setDisputeReason(reason);
        order.setAutoCompleteAt(null);
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        String itemId = order.getOrderItems().get(0).getItemId();

        // Event Sourcing
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reason", reason);
        eventStore.recordEvent(orderId, OrderEventType.ORDER_DISPUTED, buyerId, "BUYER", metadata, saved);

        // Notify
        notificationClient.sendNotification(order.getSellerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đang bị khiếu nại: \"" + reason + "\". Chờ admin xử lý.",
                "ORDER_DISPUTED", itemId);
        notificationClient.sendNotification(buyerId,
                "Khiếu nại đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã được ghi nhận. Admin sẽ xử lý sớm.",
                "ORDER_DISPUTED", itemId);

        log.info("Order {} disputed by buyer {}: {}", orderId, buyerId, reason);
        return saved;
    }

    // ====================================================================
    // SELLER: Chuẩn bị hàng
    // ====================================================================

    @Override
    @Transactional
    public Order confirmPreparing(String orderId, String sellerId) {
        Order order = getOrderForSeller(orderId, sellerId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Chỉ có thể chuẩn bị hàng khi đơn ở trạng thái 'Đã thanh toán'.");
        }

        order.setStatus(OrderStatus.PREPARING);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        String itemId = order.getOrderItems().get(0).getItemId();

        // Event Sourcing
        eventStore.recordEvent(orderId, OrderEventType.ORDER_PREPARING, sellerId, "SELLER", null, saved);

        // Notify buyer
        notificationClient.sendNotification(order.getBuyerId(),
                "Người bán đang chuẩn bị đóng gói hàng cho đơn #" + orderId.substring(0, 8).toUpperCase() + ".",
                "ORDER_PREPARING", itemId);

        log.info("Order {} → PREPARING by seller {}", orderId, sellerId);
        return saved;
    }

    // ====================================================================
    // SELLER: Giao cho shipper
    // ====================================================================

    @Override
    @Transactional
    public Order handoverToShipper(String orderId, String sellerId, Shipment shipmentData) {
        Order order = getOrderForSeller(orderId, sellerId);

        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new RuntimeException("Chỉ giao cho shipper khi đơn đang 'Chuẩn bị hàng'.");
        }

        order.setStatus(OrderStatus.HANDOVER_TO_SHIPPER);
        order.setUpdatedAt(LocalDateTime.now());

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID().toString());
        shipment.setCarrier(shipmentData.getCarrier());
        shipment.setTrackingCode(shipmentData.getTrackingCode());
        shipment.setStatus(ShipmentStatus.PREPARING);
        shipment.setShippedAt(LocalDateTime.now());
        shipment.setOrder(order);
        order.setShipment(shipment);

        Order saved = orderRepository.save(order);
        String itemId = order.getOrderItems().get(0).getItemId();

        // Event Sourcing
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("carrier", shipmentData.getCarrier());
        metadata.put("trackingCode", shipmentData.getTrackingCode());
        eventStore.recordEvent(orderId, OrderEventType.ORDER_HANDOVER, sellerId, "SELLER", metadata, saved);

        // Notify buyer
        notificationClient.sendNotification(order.getBuyerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã giao cho " +
                        shipmentData.getCarrier() + ". Mã vận đơn: " + shipmentData.getTrackingCode() + ".",
                "ORDER_HANDOVER", itemId);

        log.info("Order {} → HANDOVER_TO_SHIPPER by seller {}", orderId, sellerId);
        return saved;
    }

    // ====================================================================
    // SELLER: Hủy đơn
    // ====================================================================

    @Override
    @Transactional
    public Order cancelOrderBySeller(String orderId, String sellerId) {
        Order order = getOrderForSeller(orderId, sellerId);

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PREPARING) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi ở trạng thái 'Đã thanh toán' hoặc 'Đang chuẩn bị'.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Người bán hủy đơn");
        order.setUpdatedAt(LocalDateTime.now());

        walletClient.escrowRefund(order.getBuyerId(), order.getTotalPrice(), orderId);

        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "ACTIVE");

        Order saved = orderRepository.save(order);

        // Event Sourcing
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reason", "Người bán hủy đơn");
        eventStore.recordEvent(orderId, OrderEventType.ORDER_CANCELLED, sellerId, "SELLER", metadata, saved);
        eventStore.recordEvent(orderId, OrderEventType.ESCROW_REFUNDED, order.getBuyerId(), "SYSTEM",
                Map.of("amount", order.getTotalPrice(), "type", "REFUND"), saved);

        // Notify
        notificationClient.sendNotification(order.getBuyerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã bị người bán hủy. Tiền đã hoàn vào ví.",
                "ORDER_CANCELLED", itemId);
        notificationClient.sendNotification(sellerId,
                "Bạn đã hủy đơn hàng #" + orderId.substring(0, 8).toUpperCase() + ".",
                "ORDER_CANCELLED", itemId);

        log.info("Order {} cancelled by seller {}", orderId, sellerId);
        return saved;
    }

    // ====================================================================
    // ADMIN: Cập nhật trạng thái
    // ====================================================================

    @Override
    @Transactional
    public Order updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
        
        String oldStatus = order.getStatus().name();
        order.setStatus(OrderStatus.valueOf(status));
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        // Event Sourcing
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("oldStatus", oldStatus);
        metadata.put("newStatus", status);
        eventStore.recordEvent(orderId, OrderEventType.STATUS_UPDATED, "ADMIN", "ADMIN", metadata, saved);

        return saved;
    }

    // ====================================================================
    // ADMIN: Xử lý dispute
    // ====================================================================

    @Override
    @Transactional
    public Order resolveDispute(String orderId, String action) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        if (order.getStatus() != OrderStatus.DISPUTED) {
            throw new RuntimeException("Đơn hàng không ở trạng thái tranh chấp.");
        }

        String itemId = order.getOrderItems().get(0).getItemId();

        if ("refund".equalsIgnoreCase(action)) {
            order.setStatus(OrderStatus.CANCELLED);
            walletClient.escrowRefund(order.getBuyerId(), order.getTotalPrice(), orderId);
            itemClient.updateItemStatus(itemId, "ACTIVE");

            notificationClient.sendNotification(order.getBuyerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " được chấp nhận. Tiền đã hoàn vào ví.",
                    "ORDER_DISPUTE_RESOLVED", itemId);
            notificationClient.sendNotification(order.getSellerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " đã xử lý. Tiền hoàn cho người mua.",
                    "ORDER_DISPUTE_RESOLVED", itemId);

            // Event Sourcing
            eventStore.recordEvent(orderId, OrderEventType.ORDER_DISPUTE_RESOLVED, "ADMIN", "ADMIN",
                    Map.of("action", "refund"), order);
            eventStore.recordEvent(orderId, OrderEventType.ESCROW_REFUNDED, order.getBuyerId(), "SYSTEM",
                    Map.of("amount", order.getTotalPrice()), order);

        } else if ("release".equalsIgnoreCase(action)) {
            order.setStatus(OrderStatus.COMPLETED);
            walletClient.escrowRelease(order.getSellerId(), order.getTotalPrice(), orderId);
            itemClient.updateItemStatus(itemId, "SOLD");

            notificationClient.sendNotification(order.getSellerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " đã xử lý. Tiền đã chuyển vào ví.",
                    "ORDER_DISPUTE_RESOLVED", itemId);
            notificationClient.sendNotification(order.getBuyerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " đã được xử lý.",
                    "ORDER_DISPUTE_RESOLVED", itemId);

            // Event Sourcing
            eventStore.recordEvent(orderId, OrderEventType.ORDER_DISPUTE_RESOLVED, "ADMIN", "ADMIN",
                    Map.of("action", "release"), order);
            eventStore.recordEvent(orderId, OrderEventType.ESCROW_RELEASED, order.getSellerId(), "SYSTEM",
                    Map.of("amount", order.getTotalPrice()), order);
        } else {
            throw new RuntimeException("Action không hợp lệ. Chọn 'refund' hoặc 'release'.");
        }

        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Dispute for order {} resolved: action={}", orderId, action);
        return saved;
    }

    // ====================================================================
    // Helper
    // ====================================================================

    private void releaseReservedItem(String itemId, boolean itemReserved, Exception cause) {
        if (!itemReserved) {
            return;
        }

        try {
            itemClient.updateItemStatus(itemId, "ACTIVE");
            log.warn("Released reserved item {} after order creation failed: {}", itemId, cause.getMessage());
        } catch (Exception releaseError) {
            log.error("Failed to release reserved item {} after order creation failure", itemId, releaseError);
        }
    }

    private Order getOrderForSeller(String orderId, String sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        if (!sellerId.equals(order.getSellerId())) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn hàng này.");
        }
        return order;
    }
}