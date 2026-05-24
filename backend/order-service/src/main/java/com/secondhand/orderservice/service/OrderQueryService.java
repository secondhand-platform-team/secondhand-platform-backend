package com.secondhand.orderservice.service;

import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderEvent;

import java.util.List;
import java.util.Map;

/**
 * CQRS — Query Service Interface
 * 
 * Chứa tất cả Read operations (xem đơn, thống kê, lịch sử events).
 * Có thể optimize riêng cho read (readOnly transaction, cache, replicas).
 * 
 * Tách biệt khỏi Command để:
 * - Scale read/write độc lập
 * - Optimize read với @Transactional(readOnly = true)
 * - Dễ test riêng biệt
 */
public interface OrderQueryService {

    // ====== Buyer Queries ======
    List<Order> getOrdersByBuyerId(String buyerId);
    Order getOrderById(String orderId, String buyerId);

    // ====== Seller Queries ======
    List<Order> getOrdersBySellerId(String sellerId);

    // ====== Admin Queries ======
    List<Order> getAllOrders();
    Order getOrderByIdAdmin(String orderId);
    List<Order> getDisputedOrders();
    Map<String, Object> getAdminStatistics(String timeframe);

    // ====== Event Sourcing Queries ======
    /** Lấy toàn bộ events timeline của 1 order */
    List<OrderEvent> getOrderEvents(String orderId);
}
