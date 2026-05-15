package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderItem;
import com.secondhand.orderservice.model.Payment;
import com.secondhand.orderservice.model.Shipment;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.model.enums.PaymentMethod;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import com.secondhand.orderservice.repository.ShipmentRepository;
import com.secondhand.orderservice.service.CartService;
import com.secondhand.orderservice.service.OrderService;
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
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setAmount(totalPrice);
        payment.setMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(now);
        payment.setOrder(order);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        // Clear the cart after order is created
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            try {
                cartService.removeItemFromCart(buyerId, item.getItemId());
            } catch (Exception ignored) {
                // Cart item might not exist, ignore
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
}
