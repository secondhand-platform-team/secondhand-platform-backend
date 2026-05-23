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

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final WalletClient walletClient;
    private final ItemClient itemClient;
    private final NotificationClient notificationClient;
    private final PaymentService paymentService;
    private final com.secondhand.orderservice.repository.PaymentRepository paymentRepository;

    // ====================================================================
    // BUYER: Tạo đơn hàng
    // ====================================================================

    @Override
    @Transactional
    public Order createOrder(String buyerId, CreateOrderRequest request) {
        log.info("Creating order for buyer={}, itemId={}", buyerId, request.getItemId());

        // 1. Lấy thông tin item từ core-service
        Map<String, Object> itemData = itemClient.getItem(request.getItemId());
        String itemStatus = (String) itemData.get("status");
        String sellerId = (String) itemData.get("userId");
        String itemName = (String) itemData.get("title");
        Object priceObj = itemData.get("price");
        double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : Double.parseDouble(priceObj.toString());

        // Lấy ảnh đầu tiên làm snapshot
        String itemImageUrl = null;
        Object imagesObj = itemData.get("images");
        if (imagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> images = (List<Map<String, Object>>) imagesObj;
            if (!images.isEmpty()) {
                itemImageUrl = (String) images.get(0).get("url");
            }
        }

        // 2. Validate: item phải ACTIVE
        if (!"ACTIVE".equals(itemStatus)) {
            throw new RuntimeException("Sản phẩm không còn khả dụng (trạng thái: " + itemStatus + ")");
        }

        // 3. Không cho mua sản phẩm của chính mình
        if (buyerId.equals(sellerId)) {
            throw new RuntimeException("Bạn không thể mua sản phẩm của chính mình.");
        }

        // 4. Tạo Order
        String orderId = UUID.randomUUID().toString();

        Order order = new Order();
        order.setId(orderId);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setTotalPrice(price);
        // 6. Xử lý Payment Method
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.setPaymentStatus(PaymentStatus.PENDING);
        } else {
            order.setStatus(OrderStatus.PAID);
            order.setPaymentStatus(PaymentStatus.PAID);
            // 6. Escrow Hold — tạm giữ tiền buyer
            walletClient.escrowHold(buyerId, price, orderId);
            order.setEscrowTransactionId("ESCROW-HOLD-" + orderId);
        }

        // 7. Cập nhật item → RESERVED
        itemClient.updateItemStatus(request.getItemId(), "RESERVED");

        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // 5. Tạo OrderItem
        OrderItem orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID().toString());
        orderItem.setItemId(request.getItemId());
        orderItem.setItemName(itemName);
        orderItem.setSellerId(sellerId);
        orderItem.setPrice(price);
        orderItem.setItemImageUrl(itemImageUrl);
        orderItem.setOrder(order);
        order.setOrderItems(List.of(orderItem));

        // 8. Lưu order
        Order savedOrder = orderRepository.save(order);

        // 8b. Nếu VNPAY thì tạo payment URL
        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            com.secondhand.orderservice.dto.response.PaymentResponse payRes = 
                paymentService.createVnPayPaymentInternal((long)price, null, "vn", buyerId, null);
            savedOrder.setPaymentUrl(payRes.getPaymentUrl());
            
            // Link Payment to Order
            paymentRepository.findByTransactionId(payRes.getTransactionId()).ifPresent(payment -> {
                payment.setOrder(savedOrder);
                paymentRepository.save(payment);
            });
        }

        // 9. Xóa item khỏi cart buyer
        try {
            cartService.removeItemFromCart(buyerId, request.getItemId());
        } catch (Exception e) {
            log.warn("Failed to remove item from cart: {}", e.getMessage());
        }

        // 10. Notify Buyer
        notificationClient.sendNotification(
                buyerId,
                "Đặt hàng thành công! Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đang chờ người bán xử lý.",
                "ORDER_CREATED",
                request.getItemId()
        );

        // 11. Notify Seller
        notificationClient.sendNotification(
                sellerId,
                "Sản phẩm \"" + itemName + "\" đã được đặt mua! Vui lòng chuẩn bị hàng.",
                "ORDER_NEW_FOR_SELLER",
                request.getItemId()
        );

        log.info("Order {} created successfully, escrow held for buyer={}", orderId, buyerId);
        return savedOrder;
    }

    // ====================================================================
    // BUYER: Xem đơn hàng
    // ====================================================================

    @Override
    public List<Order> getOrdersByBuyerId(String buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Override
    public Order getOrderById(String orderId, String buyerId) {
        return orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
    }

    // ====================================================================
    // BUYER: Hủy đơn
    // ====================================================================

    @Override
    @Transactional
    public Order cancelOrder(String orderId, String buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        // Chỉ cancel được khi PAID
        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi đang ở trạng thái 'Đã thanh toán'.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Người mua hủy đơn");
        order.setUpdatedAt(LocalDateTime.now());

        // Escrow refund
        walletClient.escrowRefund(buyerId, order.getTotalPrice(), orderId);

        // Item → ACTIVE
        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "ACTIVE");

        Order saved = orderRepository.save(order);

        // Notify seller
        notificationClient.sendNotification(
                order.getSellerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã bị người mua hủy.",
                "ORDER_CANCELLED",
                itemId
        );

        // Notify buyer
        notificationClient.sendNotification(
                buyerId,
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã hủy thành công. Tiền đã hoàn vào ví.",
                "ORDER_CANCELLED",
                itemId
        );

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

        // Escrow release → seller wallet
        walletClient.escrowRelease(order.getSellerId(), order.getTotalPrice(), orderId);

        // Item → SOLD
        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "SOLD");

        Order saved = orderRepository.save(order);

        // Notify seller
        notificationClient.sendNotification(
                order.getSellerId(),
                "Người mua đã xác nhận nhận hàng! " + (long) order.getTotalPrice().doubleValue() + " VNĐ đã chuyển vào ví của bạn.",
                "ORDER_COMPLETED",
                itemId
        );

        // Notify buyer
        notificationClient.sendNotification(
                buyerId,
                "Cảm ơn bạn! Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã hoàn tất.",
                "ORDER_COMPLETED",
                itemId
        );

        log.info("Order {} confirmed received by buyer {}, escrow released to seller {}", orderId, buyerId, order.getSellerId());
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
        order.setAutoCompleteAt(null); // dừng auto-complete
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        String itemId = order.getOrderItems().get(0).getItemId();

        // Notify seller
        notificationClient.sendNotification(
                order.getSellerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đang bị khiếu nại: \"" + reason + "\". Chờ admin xử lý.",
                "ORDER_DISPUTED",
                itemId
        );

        // Notify buyer
        notificationClient.sendNotification(
                buyerId,
                "Khiếu nại đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã được ghi nhận. Admin sẽ xử lý sớm.",
                "ORDER_DISPUTED",
                itemId
        );

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

        // Notify buyer
        notificationClient.sendNotification(
                order.getBuyerId(),
                "Người bán đang chuẩn bị đóng gói hàng cho đơn #" + orderId.substring(0, 8).toUpperCase() + ".",
                "ORDER_PREPARING",
                itemId
        );

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

        // Tạo Shipment
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

        // Notify buyer
        notificationClient.sendNotification(
                order.getBuyerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã giao cho " +
                        shipmentData.getCarrier() + ". Mã vận đơn: " + shipmentData.getTrackingCode() + ".",
                "ORDER_HANDOVER",
                itemId
        );

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

        // Escrow refund → buyer
        walletClient.escrowRefund(order.getBuyerId(), order.getTotalPrice(), orderId);

        // Item → ACTIVE
        String itemId = order.getOrderItems().get(0).getItemId();
        itemClient.updateItemStatus(itemId, "ACTIVE");

        Order saved = orderRepository.save(order);

        // Notify buyer
        notificationClient.sendNotification(
                order.getBuyerId(),
                "Đơn hàng #" + orderId.substring(0, 8).toUpperCase() + " đã bị người bán hủy. Tiền đã hoàn vào ví.",
                "ORDER_CANCELLED",
                itemId
        );

        // Notify seller
        notificationClient.sendNotification(
                sellerId,
                "Bạn đã hủy đơn hàng #" + orderId.substring(0, 8).toUpperCase() + ".",
                "ORDER_CANCELLED",
                itemId
        );

        log.info("Order {} cancelled by seller {}", orderId, sellerId);
        return saved;
    }

    // ====================================================================
    // SELLER: Xem đơn hàng
    // ====================================================================

    @Override
    public List<Order> getOrdersBySellerId(String sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }

    // ====================================================================
    // ADMIN
    // ====================================================================

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Order getOrderByIdAdmin(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
    }

    @Override
    @Transactional
    public Order updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
        order.setStatus(OrderStatus.valueOf(status));
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getDisputedOrders() {
        return orderRepository.findByStatusOrderByUpdatedAtDesc(OrderStatus.DISPUTED);
    }

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
            // Hoàn tiền cho buyer
            order.setStatus(OrderStatus.CANCELLED);
            walletClient.escrowRefund(order.getBuyerId(), order.getTotalPrice(), orderId);
            itemClient.updateItemStatus(itemId, "ACTIVE");

            notificationClient.sendNotification(
                    order.getBuyerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " được chấp nhận. Tiền đã hoàn vào ví.",
                    "ORDER_DISPUTE_RESOLVED",
                    itemId
            );
            notificationClient.sendNotification(
                    order.getSellerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " đã xử lý. Tiền hoàn cho người mua.",
                    "ORDER_DISPUTE_RESOLVED",
                    itemId
            );
        } else if ("release".equalsIgnoreCase(action)) {
            // Release tiền cho seller
            order.setStatus(OrderStatus.COMPLETED);
            walletClient.escrowRelease(order.getSellerId(), order.getTotalPrice(), orderId);
            itemClient.updateItemStatus(itemId, "SOLD");

            notificationClient.sendNotification(
                    order.getSellerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " đã xử lý. Tiền đã chuyển vào ví.",
                    "ORDER_DISPUTE_RESOLVED",
                    itemId
            );
            notificationClient.sendNotification(
                    order.getBuyerId(),
                    "Khiếu nại #" + orderId.substring(0, 8).toUpperCase() + " đã được xử lý.",
                    "ORDER_DISPUTE_RESOLVED",
                    itemId
            );
        } else {
            throw new RuntimeException("Action không hợp lệ. Chọn 'refund' hoặc 'release'.");
        }

        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Dispute for order {} resolved: action={}", orderId, action);
        return saved;
    }

    @Override
    public Map<String, Object> getAdminStatistics(String timeframe) {
        LocalDateTime startDate;
        switch (timeframe) {
            case "week":
                startDate = LocalDateTime.now().minusWeeks(1);
                break;
            case "year":
                startDate = LocalDateTime.now().minusYears(1);
                break;
            case "month":
            default:
                startDate = LocalDateTime.now().minusMonths(1);
                break;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", orderRepository.getTotalRevenue(startDate));
        stats.put("totalOrders", orderRepository.getTotalOrders(startDate));
        stats.put("revenueByTimeframe", orderRepository.getRevenueByTimeframe(startDate));
        stats.put("ordersByTimeframe", orderRepository.getOrdersByTimeframe(startDate));

        return stats;
    }

    // ====================================================================
    // Helper
    // ====================================================================

    private Order getOrderForSeller(String orderId, String sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));

        if (!sellerId.equals(order.getSellerId())) {
            throw new RuntimeException("Bạn không có quyền thao tác đơn hàng này.");
        }
        return order;
    }
}
