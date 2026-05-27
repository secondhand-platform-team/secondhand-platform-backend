package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderEvent;
import com.secondhand.orderservice.model.Shipment;
import com.secondhand.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * CQRS — Facade Service Implementation
 * 
 * Delegates tất cả commands → OrderCommandServiceImpl
 * Delegates tất cả queries → OrderQueryServiceImpl
 * 
 * Đây là Facade Pattern kết hợp CQRS:
 * - Controller chỉ cần inject 1 OrderService
 * - Bên trong, write/read được tách riêng hoàn toàn
 * - Có thể scale read/write độc lập (future: read replica)
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderCommandServiceImpl commandService;
    private final OrderQueryServiceImpl queryService;

    // ====== Command delegates ======

    @Override
    public Order createOrder(String buyerId, CreateOrderRequest request) {
        return commandService.createOrder(buyerId, request);
    }

    @Override
    public Order cancelOrder(String orderId, String buyerId) {
        return commandService.cancelOrder(orderId, buyerId);
    }

    @Override
    public Order confirmReceived(String orderId, String buyerId) {
        return commandService.confirmReceived(orderId, buyerId);
    }

    @Override
    public Order disputeOrder(String orderId, String buyerId, String reason) {
        return commandService.disputeOrder(orderId, buyerId, reason);
    }

    @Override
    public Order confirmPreparing(String orderId, String sellerId) {
        return commandService.confirmPreparing(orderId, sellerId);
    }

    @Override
    public Order handoverToShipper(String orderId, String sellerId, Shipment shipmentData) {
        return commandService.handoverToShipper(orderId, sellerId, shipmentData);
    }

    @Override
    public Order cancelOrderBySeller(String orderId, String sellerId) {
        return commandService.cancelOrderBySeller(orderId, sellerId);
    }

    @Override
    public Order updateOrderStatus(String orderId, String status) {
        return commandService.updateOrderStatus(orderId, status);
    }

    @Override
    public Order resolveDispute(String orderId, String action) {
        return commandService.resolveDispute(orderId, action);
    }

    // ====== Query delegates ======

    @Override
    public List<Order> getOrdersByBuyerId(String buyerId) {
        return queryService.getOrdersByBuyerId(buyerId);
    }

    @Override
    public Order getOrderById(String orderId, String buyerId) {
        return queryService.getOrderById(orderId, buyerId);
    }

    @Override
    public List<Order> getOrdersBySellerId(String sellerId) {
        return queryService.getOrdersBySellerId(sellerId);
    }

    @Override
    public List<Order> getAllOrders() {
        return queryService.getAllOrders();
    }

    @Override
    public Order getOrderByIdAdmin(String orderId) {
        return queryService.getOrderByIdAdmin(orderId);
    }

    @Override
    public List<Order> getDisputedOrders() {
        return queryService.getDisputedOrders();
    }

    @Override
    public Map<String, Object> getAdminStatistics(String timeframe) {
        return queryService.getAdminStatistics(timeframe);
    }

    @Override
    public List<OrderEvent> getOrderEvents(String orderId) {
        return queryService.getOrderEvents(orderId);
    }
}
