package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.dto.response.PaymentResponse;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderItem;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.model.Shipment;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.model.enums.PaymentMethod;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import com.secondhand.orderservice.repository.PaymentRepository;
import com.secondhand.orderservice.repository.ShipmentRepository;
import com.secondhand.orderservice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final CartService cartService;
    private final NotificationClient notificationClient;
    private final WalletClient walletClient;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Order createOrder(String buyerId, CreateOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setId(orderId);
        order.setBuyerId(buyerId);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        double totalPrice = 0;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(UUID.randomUUID().toString());
            orderItem.setItemId(itemRequest.getItemId());
            orderItem.setItemName(itemRequest.getItemName());
            orderItem.setSellerId(itemRequest.getSellerId());
            orderItem.setPrice(itemRequest.getPrice());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setOrder(order);
            orderItems.add(orderItem);

            totalPrice += itemRequest.getPrice() * itemRequest.getQuantity();
        }

        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);

        // Create payment record
        Payment payment;
        if (PaymentMethod.VNPAY.name().equals(request.getPaymentMethod())) {
            String customReturnUrl = "http://localhost:8000/order/api/payment/vnpay-callback";
            PaymentResponse vnpayResp = paymentService.createVnPayPaymentInternal(
                (long) totalPrice,
                "NCB",
                "vn",
                buyerId,
                customReturnUrl
            );
            if (!"00".equals(vnpayResp.getCode())) {
                throw new RuntimeException("Không thể tạo liên kết thanh toán VNPay: " + vnpayResp.getMessage());
            }
            
            payment = paymentRepository.findByTransactionId(vnpayResp.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi thanh toán VNPay vừa được tạo"));
            payment.setOrder(order);
            order.setPayment(payment);
            order.setPaymentUrl(vnpayResp.getPaymentUrl());
        } else if (PaymentMethod.WALLET.name().equals(request.getPaymentMethod())) {
            // Trừ tiền người mua
            walletClient.deduct(buyerId, totalPrice, "Thanh toán đơn hàng #" + orderId.substring(0, 8).toUpperCase());
            
            // Cộng tiền cho người bán của từng sản phẩm trong đơn hàng
            for (OrderItem item : orderItems) {
                if (item.getSellerId() != null && !item.getSellerId().isEmpty()) {
                    double earnAmount = item.getPrice() * item.getQuantity();
                    walletClient.add(
                        item.getSellerId(), 
                        earnAmount, 
                        "Bán sản phẩm \"" + item.getItemName() + "\" thuộc đơn hàng #" + orderId.substring(0, 8).toUpperCase()
                    );
                }
            }
            
            order.setStatus(OrderStatus.PAID);
            order.setPaymentStatus(PaymentStatus.PAID);
            
            payment = new Payment();
            payment.setId(UUID.randomUUID().toString());
            payment.setTransactionId("WALLET-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            payment.setAmount(totalPrice);
            payment.setMethod(PaymentMethod.WALLET);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(now);
            payment.setCreatedAt(now);
            payment.setOrder(order);
            order.setPayment(payment);
        } else {
            payment = new Payment();
            payment.setId(UUID.randomUUID().toString());
            payment.setAmount(totalPrice);
            payment.setMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(now);
            payment.setOrder(order);
            order.setPayment(payment);
        }

        Order savedOrder = orderRepository.save(order);
        if (order.getPaymentUrl() != null) {
            savedOrder.setPaymentUrl(order.getPaymentUrl());
        }

        // Clear the cart after order is created (Only if NOT VNPAY)
        if (!PaymentMethod.VNPAY.name().equals(request.getPaymentMethod())) {
            for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
                try {
                    cartService.removeItemFromCart(buyerId, item.getItemId());
                } catch (Exception ignored) {
                    // Cart item might not exist, ignore
                }
            }
        }

        // Only send notifications immediately if payment method is NOT VNPAY (e.g., WALLET)
        if (!PaymentMethod.VNPAY.name().equals(request.getPaymentMethod())) {
            // Notify buyer
            try {
                notificationClient.sendNotification(
                    buyerId,
                    "Cảm ơn bạn đã đặt hàng, chúng tôi sẽ liên hệ người bán sớm nhất có thể",
                    "ORDER_CREATED",
                    null
                );
            } catch (Exception e) {
                // Ignored, don't rollback if notification fails
            }

            // Notify sellers
            try {
                java.util.Set<String> sellerIds = new java.util.HashSet<>();
                for (OrderItem item : savedOrder.getOrderItems()) {
                    if (item.getSellerId() != null) {
                        sellerIds.add(item.getSellerId());
                    }
                }
                for (String sellerId : sellerIds) {
                    String itemId = savedOrder.getOrderItems().stream()
                        .filter(i -> sellerId.equals(i.getSellerId()))
                        .map(OrderItem::getItemId)
                        .findFirst()
                        .orElse(null);

                    notificationClient.sendNotification(
                        sellerId,
                        "Sản phẩm của bạn đã được user " + buyerId + " mua, vui lòng kiểm tra đơn hàng và giao hàng đúng hạn",
                        "ORDER_CREATED",
                        itemId
                    );
                }
            } catch (Exception e) {
                // Ignored
            }
        }

        return savedOrder;

    }

    @Override
    public List<Order> getOrdersByBuyerId(String buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Override
    public Order getOrderById(String orderId, String buyerId) {
        return orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
    }

    @Override
    @Transactional
    public Order cancelOrder(String orderId, String buyerId) {
        Order order = getOrderById(orderId, buyerId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái chờ xử lý");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order returnOrder(String orderId, String buyerId) {
        Order order = getOrderById(orderId, buyerId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Chỉ có thể trả hàng khi đơn hàng đã giao thành công");
        }
        order.setStatus(OrderStatus.RETURNED);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    // ==================== ADMIN ====================

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Order getOrderByIdAdmin(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
    }

    @Override
    @Transactional
    public Order updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        OrderStatus newStatus = OrderStatus.valueOf(status);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Auto-update payment status based on order status
        if (newStatus == OrderStatus.PAID && order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.PAID);
            order.getPayment().setPaidAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        if (newStatus == OrderStatus.DELIVERED && order.getShipment() != null) {
            order.getShipment().setStatus("DELIVERED");
            order.getShipment().setDeliveredAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order createShipment(String orderId, Shipment shipmentData) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID().toString());
        shipment.setCarrier(shipmentData.getCarrier());
        shipment.setTrackingCode(shipmentData.getTrackingCode());
        shipment.setStatus("PREPARING");
        shipment.setShippedAt(LocalDateTime.now());
        shipment.setOrder(order);
        order.setShipment(shipment);

        // Auto confirm + set shipping
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updateShipment(String orderId, Shipment shipmentData) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Shipment shipment = order.getShipment();
        if (shipment == null) {
            throw new RuntimeException("Đơn hàng chưa có thông tin vận chuyển");
        }

        if (shipmentData.getCarrier() != null) shipment.setCarrier(shipmentData.getCarrier());
        if (shipmentData.getTrackingCode() != null) shipment.setTrackingCode(shipmentData.getTrackingCode());
        if (shipmentData.getStatus() != null) {
            shipment.setStatus(shipmentData.getStatus());
            if ("SHIPPING".equals(shipmentData.getStatus())) {
                shipment.setShippedAt(LocalDateTime.now());
                order.setStatus(OrderStatus.SHIPPING);
            }
            if ("DELIVERED".equals(shipmentData.getStatus())) {
                shipment.setDeliveredAt(LocalDateTime.now());
                order.setStatus(OrderStatus.DELIVERED);
            }
        }
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    public java.util.Map<String, Object> getAdminStatistics(String timeframe) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        java.time.LocalDateTime startDate;
        switch (timeframe.toLowerCase()) {
            case "day":
                startDate = java.time.LocalDateTime.now().with(java.time.LocalTime.MIN);
                break;
            case "week":
                startDate = java.time.LocalDateTime.now().minusDays(7);
                break;
            case "year":
                startDate = java.time.LocalDateTime.now().minusYears(1);
                break;
            case "month":
            default:
                startDate = java.time.LocalDateTime.now().minusDays(30);
                break;
        }

        Double totalRevenue = orderRepository.getTotalRevenue(startDate);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        
        Long totalOrders = orderRepository.getTotalOrders(startDate);
        stats.put("totalOrders", totalOrders != null ? totalOrders : 0);
        
        stats.put("dailyRevenue", orderRepository.getRevenueByTimeframe(startDate));
        stats.put("dailyOrders", orderRepository.getOrdersByTimeframe(startDate));
        stats.put("topSellers", orderRepository.getTopSellersByTimeframe(startDate));
        stats.put("topProducts", orderRepository.getTopProductsByTimeframe(startDate));
        
        return stats;
    }

    private void validateOrderSeller(Order order, String sellerId) {
        boolean isSeller = order.getOrderItems().stream()
                .anyMatch(item -> sellerId.equals(item.getSellerId()));
        if (!isSeller) {
            throw new RuntimeException("Bạn không có quyền quản lý đơn hàng này");
        }
    }

    @Override
    public List<Order> getOrdersBySellerId(String sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }

    @Override
    @Transactional
    public Order cancelOrderBySeller(String orderId, String sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        validateOrderSeller(order, sellerId);

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng ở trạng thái chờ xử lý hoặc đã xác nhận");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        // Notify Buyer about cancellation
        try {
            notificationClient.sendNotification(
                order.getBuyerId(),
                "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đã bị người bán hủy.",
                "ORDER_STATUS",
                order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getItemId()
            );
        } catch (Exception ignored) {}

        return savedOrder;
    }

    @Override
    @Transactional
    public Order updateOrderStatusBySeller(String orderId, String sellerId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        validateOrderSeller(order, sellerId);

        OrderStatus newStatus = OrderStatus.valueOf(status);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        // Auto-update payment status based on order status
        if (newStatus == OrderStatus.PAID && order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.PAID);
            order.getPayment().setPaidAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        if (newStatus == OrderStatus.DELIVERED && order.getShipment() != null) {
            order.getShipment().setStatus("DELIVERED");
            order.getShipment().setDeliveredAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);

        // Notify Buyer about status update
        try {
            String content = "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đã cập nhật trạng thái: " + getStatusLabel(newStatus);
            notificationClient.sendNotification(
                order.getBuyerId(),
                content,
                "ORDER_STATUS",
                order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getItemId()
            );
        } catch (Exception ignored) {}

        return savedOrder;
    }

    @Override
    @Transactional
    public Order createShipmentBySeller(String orderId, String sellerId, Shipment shipmentData) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        validateOrderSeller(order, sellerId);

        Shipment shipment = new Shipment();
        shipment.setId(UUID.randomUUID().toString());
        shipment.setCarrier(shipmentData.getCarrier());
        shipment.setTrackingCode(shipmentData.getTrackingCode());
        shipment.setStatus("PREPARING");
        shipment.setShippedAt(LocalDateTime.now());
        shipment.setOrder(order);
        order.setShipment(shipment);

        // Auto confirm + set shipping
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Notify Buyer about shipment creation
        try {
            notificationClient.sendNotification(
                order.getBuyerId(),
                "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đã được chuẩn bị vận chuyển qua đơn vị " + shipmentData.getCarrier() + ". Mã vận đơn: " + shipmentData.getTrackingCode(),
                "ORDER_STATUS",
                order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getItemId()
            );
        } catch (Exception ignored) {}

        return savedOrder;
    }

    @Override
    @Transactional
    public Order updateShipmentBySeller(String orderId, String sellerId, Shipment shipmentData) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        validateOrderSeller(order, sellerId);

        Shipment shipment = order.getShipment();
        if (shipment == null) {
            throw new RuntimeException("Đơn hàng chưa có thông tin vận chuyển");
        }

        if (shipmentData.getCarrier() != null) shipment.setCarrier(shipmentData.getCarrier());
        if (shipmentData.getTrackingCode() != null) shipment.setTrackingCode(shipmentData.getTrackingCode());
        if (shipmentData.getStatus() != null) {
            shipment.setStatus(shipmentData.getStatus());
            if ("SHIPPING".equals(shipmentData.getStatus())) {
                shipment.setShippedAt(LocalDateTime.now());
                order.setStatus(OrderStatus.SHIPPING);
            }
            if ("DELIVERED".equals(shipmentData.getStatus())) {
                shipment.setDeliveredAt(LocalDateTime.now());
                order.setStatus(OrderStatus.DELIVERED);
            }
        }
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // Notify Buyer about shipping status update
        try {
            String shippingStatus = shipmentData.getStatus();
            String content = "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đang được giao. Đơn vị: " + shipment.getCarrier();
            if ("DELIVERED".equals(shippingStatus)) {
                content = "Đơn hàng #" + order.getId().substring(0, 8).toUpperCase() + " đã giao thành công!";
            }
            notificationClient.sendNotification(
                order.getBuyerId(),
                content,
                "ORDER_STATUS",
                order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getItemId()
            );
        } catch (Exception ignored) {}

        return savedOrder;
    }

    private String getStatusLabel(OrderStatus status) {
        switch(status) {
            case PENDING: return "Chờ xử lý";
            case CONFIRMED: return "Đã xác nhận";
            case PAID: return "Đã thanh toán";
            case SHIPPING: return "Đang giao";
            case DELIVERED: return "Đã giao thành công";
            case CANCELLED: return "Đã hủy";
            case RETURNED: return "Trả hàng";
            default: return status.name();
        }
    }
}
