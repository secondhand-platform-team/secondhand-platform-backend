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
 * CQRS â€” Command Service Implementation
 * 
 * Chá»©a táº¥t cáº£ Write operations (táº¡o, sá»­a, há»§y, chuyá»ƒn tráº¡ng thÃ¡i).
 * Sau má»—i state change â†’ ghi OrderEvent (Event Sourcing).
 * 
 * Pattern Ã¡p dá»¥ng:
 * - CQRS: TÃ¡ch riÃªng Command khá»i Query
 * - Event Sourcing: Ghi láº¡i má»i thay Ä‘á»•i thÃ nh immutable events
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
    // BUYER: Táº¡o Ä‘Æ¡n hÃ ng
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
                    paymentService.createVnPayPaymentInternal((long) price, null, "vn", buyerId, null);
                savedOrder.setPaymentUrl(payRes.getPaymentUrl());

                paymentRepository.findByTransactionId(payRes.getTransactionId()).ifPresent(payment -> {
                    payment.setOrder(savedOrder);
                    paymentRepository.save(payment);
                });

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
    // BUYER: Há»§y Ä‘Æ¡n
    // ====================================================================

    @Override
    @Transactional
    public Order cancelOrder(String orderId, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("ÄÆ¡n hÃ ng khÃ´ng tá»“n táº¡i."));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Chá»‰ cÃ³ thá»ƒ há»§y Ä‘Æ¡n khi Ä‘ang á»Ÿ tráº¡ng thÃ¡i 'ÄÃ£ thanh toÃ¡n'.");
        }

        String oldStatus = order.getStatus().name();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("NgÆ°á»i mua há»§y Ä‘Æ¡n");
        order.setUpdatedAt(LocalDateTime.now());

        walletClient.escrowRefund(buyerId, order.getTotalPrice(), orderId);

        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "ACTIVE");

        Order saved = orderRepository.save(order);

        // Event Sourcing
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("oldStatus", oldStatus);
        metadata.put("reason", "NgÆ°á»i mua há»§y Ä‘Æ¡n");
        eventStore.recordEvent(orderId, OrderEventType.ORDER_CANCELLED, buyerId, "BUYER", metadata, saved);

        Map<String, Object> refundMeta = new LinkedHashMap<>();
        refundMeta.put("amount", order.getTotalPrice());
        refundMeta.put("type", "REFUND");
        eventStore.recordEvent(orderId, OrderEventType.ESCROW_REFUNDED, buyerId, "SYSTEM", refundMeta, saved);

        // Notify
        notificationClient.sendNotification(order.getSellerId(),
                "ÄÆ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ bá»‹ ngÆ°á»i mua há»§y.",
                "ORDER_CANCELLED", itemId);
        notificationClient.sendNotification(buyerId,
                "ÄÆ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ há»§y thÃ nh cÃ´ng. Tiá»n Ä‘Ã£ hoÃ n vÃ o vÃ­.",
                "ORDER_CANCELLED", itemId);

        log.info("Order {} cancelled by buyer {}", orderId, buyerId);
        return saved;
    }

    // ====================================================================
    // BUYER: XÃ¡c nháº­n nháº­n hÃ ng
    // ====================================================================

    @Override
    @Transactional
    public Order confirmReceived(String orderId, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("ÄÆ¡n hÃ ng khÃ´ng tá»“n táº¡i."));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Chá»‰ cÃ³ thá»ƒ xÃ¡c nháº­n nháº­n hÃ ng khi Ä‘Æ¡n Ä‘Ã£ giao.");
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
                "NgÆ°á»i mua Ä‘Ã£ xÃ¡c nháº­n nháº­n hÃ ng! " + (long) order.getTotalPrice().doubleValue() + " VNÄ Ä‘Ã£ chuyá»ƒn vÃ o vÃ­ cá»§a báº¡n.",
                "ORDER_COMPLETED", itemId);
        notificationClient.sendNotification(buyerId,
                "Cáº£m Æ¡n báº¡n! ÄÆ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ hoÃ n táº¥t.",
                "ORDER_COMPLETED", itemId);

        log.info("Order {} confirmed received by buyer {}", orderId, buyerId);
        return saved;
    }

    // ====================================================================
    // BUYER: Khiáº¿u náº¡i
    // ====================================================================

    @Override
    @Transactional
    public Order disputeOrder(String orderId, String buyerId, String reason) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("ÄÆ¡n hÃ ng khÃ´ng tá»“n táº¡i."));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Chá»‰ cÃ³ thá»ƒ khiáº¿u náº¡i khi Ä‘Æ¡n Ä‘Ã£ giao.");
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
                "ÄÆ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + " Ä‘ang bá»‹ khiáº¿u náº¡i: \"" + reason + "\". Chá» admin xá»­ lÃ½.",
                "ORDER_DISPUTED", itemId);
        notificationClient.sendNotification(buyerId,
                "Khiáº¿u náº¡i Ä‘Æ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ Ä‘Æ°á»£c ghi nháº­n. Admin sáº½ xá»­ lÃ½ sá»›m.",
                "ORDER_DISPUTED", itemId);

        log.info("Order {} disputed by buyer {}: {}", orderId, buyerId, reason);
        return saved;
    }

    // ====================================================================
    // SELLER: Chuáº©n bá»‹ hÃ ng
    // ====================================================================

    @Override
    @Transactional
    public Order confirmPreparing(String orderId, String sellerId) {
        Order order = getOrderForSeller(orderId, sellerId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Chá»‰ cÃ³ thá»ƒ chuáº©n bá»‹ hÃ ng khi Ä‘Æ¡n á»Ÿ tráº¡ng thÃ¡i 'ÄÃ£ thanh toÃ¡n'.");
        }

        order.setStatus(OrderStatus.PREPARING);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        String itemId = order.getOrderItems().get(0).getItemId();

        // Event Sourcing
        eventStore.recordEvent(orderId, OrderEventType.ORDER_PREPARING, sellerId, "SELLER", null, saved);

        // Notify buyer
        notificationClient.sendNotification(order.getBuyerId(),
                "NgÆ°á»i bÃ¡n Ä‘ang chuáº©n bá»‹ Ä‘Ã³ng gÃ³i hÃ ng cho Ä‘Æ¡n #" + orderId.substring(0, 8).toUpperCase() + ".",
                "ORDER_PREPARING", itemId);

        log.info("Order {} â†’ PREPARING by seller {}", orderId, sellerId);
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
            throw new RuntimeException("Chá»‰ giao cho shipper khi Ä‘Æ¡n Ä‘ang 'Chuáº©n bá»‹ hÃ ng'.");
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
                "ÄÆ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ giao cho " +
                        shipmentData.getCarrier() + ". MÃ£ váº­n Ä‘Æ¡n: " + shipmentData.getTrackingCode() + ".",
                "ORDER_HANDOVER", itemId);

        log.info("Order {} â†’ HANDOVER_TO_SHIPPER by seller {}", orderId, sellerId);
        return saved;
    }

    // ====================================================================
    // SELLER: Há»§y Ä‘Æ¡n
    // ====================================================================

    @Override
    @Transactional
    public Order cancelOrderBySeller(String orderId, String sellerId) {
        Order order = getOrderForSeller(orderId, sellerId);

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PREPARING) {
            throw new RuntimeException("Chá»‰ cÃ³ thá»ƒ há»§y Ä‘Æ¡n khi á»Ÿ tráº¡ng thÃ¡i 'ÄÃ£ thanh toÃ¡n' hoáº·c 'Äang chuáº©n bá»‹'.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("NgÆ°á»i bÃ¡n há»§y Ä‘Æ¡n");
        order.setUpdatedAt(LocalDateTime.now());

        walletClient.escrowRefund(order.getBuyerId(), order.getTotalPrice(), orderId);

        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "ACTIVE");

        Order saved = orderRepository.save(order);

        // Event Sourcing
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reason", "NgÆ°á»i bÃ¡n há»§y Ä‘Æ¡n");
        eventStore.recordEvent(orderId, OrderEventType.ORDER_CANCELLED, sellerId, "SELLER", metadata, saved);
        eventStore.recordEvent(orderId, OrderEventType.ESCROW_REFUNDED, order.getBuyerId(), "SYSTEM",
                Map.of("amount", order.getTotalPrice(), "type", "REFUND"), saved);

        // Notify
        notificationClient.sendNotification(order.getBuyerId(),
                "ÄÆ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ bá»‹ ngÆ°á»i bÃ¡n há»§y. Tiá»n Ä‘Ã£ hoÃ n vÃ o vÃ­.",
                "ORDER_CANCELLED", itemId);
        notificationClient.sendNotification(sellerId,
                "Báº¡n Ä‘Ã£ há»§y Ä‘Æ¡n hÃ ng #" + orderId.substring(0, 8).toUpperCase() + ".",
                "ORDER_CANCELLED", itemId);

        log.info("Order {} cancelled by seller {}", orderId, sellerId);
        return saved;
    }

    // ====================================================================
    // ADMIN: Cáº­p nháº­t tráº¡ng thÃ¡i
    // ====================================================================

    @Override
    @Transactional
    public Order updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ÄÆ¡n hÃ ng khÃ´ng tá»“n táº¡i."));
        
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
    // ADMIN: Xá»­ lÃ½ dispute
    // ====================================================================

    @Override
    @Transactional
    public Order resolveDispute(String orderId, String action) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("ÄÆ¡n hÃ ng khÃ´ng tá»“n táº¡i."));

        if (order.getStatus() != OrderStatus.DISPUTED) {
            throw new RuntimeException("ÄÆ¡n hÃ ng khÃ´ng á»Ÿ tráº¡ng thÃ¡i tranh cháº¥p.");
        }

        String itemId = order.getOrderItems().get(0).getItemId();

        if ("refund".equalsIgnoreCase(action)) {
            order.setStatus(OrderStatus.CANCELLED);
            walletClient.escrowRefund(order.getBuyerId(), order.getTotalPrice(), orderId);
            itemClient.updateItemStatus(itemId, "ACTIVE");

            notificationClient.sendNotification(order.getBuyerId(),
                    "Khiáº¿u náº¡i #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Æ°á»£c cháº¥p nháº­n. Tiá»n Ä‘Ã£ hoÃ n vÃ o vÃ­.",
                    "ORDER_DISPUTE_RESOLVED", itemId);
            notificationClient.sendNotification(order.getSellerId(),
                    "Khiáº¿u náº¡i #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ xá»­ lÃ½. Tiá»n hoÃ n cho ngÆ°á»i mua.",
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
                    "Khiáº¿u náº¡i #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ xá»­ lÃ½. Tiá»n Ä‘Ã£ chuyá»ƒn vÃ o vÃ­.",
                    "ORDER_DISPUTE_RESOLVED", itemId);
            notificationClient.sendNotification(order.getBuyerId(),
                    "Khiáº¿u náº¡i #" + orderId.substring(0, 8).toUpperCase() + " Ä‘Ã£ Ä‘Æ°á»£c xá»­ lÃ½.",
                    "ORDER_DISPUTE_RESOLVED", itemId);

            // Event Sourcing
            eventStore.recordEvent(orderId, OrderEventType.ORDER_DISPUTE_RESOLVED, "ADMIN", "ADMIN",
                    Map.of("action", "release"), order);
            eventStore.recordEvent(orderId, OrderEventType.ESCROW_RELEASED, order.getSellerId(), "SYSTEM",
                    Map.of("amount", order.getTotalPrice()), order);
        } else {
            throw new RuntimeException("Action khÃ´ng há»£p lá»‡. Chá»n 'refund' hoáº·c 'release'.");
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
                .orElseThrow(() -> new RuntimeException("ÄÆ¡n hÃ ng khÃ´ng tá»“n táº¡i."));

        if (!sellerId.equals(order.getSellerId())) {
            throw new RuntimeException("Báº¡n khÃ´ng cÃ³ quyá»n thao tÃ¡c Ä‘Æ¡n hÃ ng nÃ y.");
        }
        return order;
    }
}


