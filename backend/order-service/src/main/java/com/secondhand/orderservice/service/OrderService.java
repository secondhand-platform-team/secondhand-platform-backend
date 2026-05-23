package com.secondhand.orderservice.service;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.Shipment;

import java.util.List;
import java.util.Map;

public interface OrderService {

    // ====== Buyer Operations ======
    Order createOrder(String buyerId, CreateOrderRequest request);
    List<Order> getOrdersByBuyerId(String buyerId);
    Order getOrderById(String orderId, String buyerId);
    Order cancelOrder(String orderId, String buyerId);
    Order confirmReceived(String orderId, String buyerId);
    Order disputeOrder(String orderId, String buyerId, String reason);

    // ====== Seller Operations ======
    List<Order> getOrdersBySellerId(String sellerId);
    Order confirmPreparing(String orderId, String sellerId);
    Order handoverToShipper(String orderId, String sellerId, Shipment shipmentData);
    Order cancelOrderBySeller(String orderId, String sellerId);

    // ====== Admin Operations ======
    List<Order> getAllOrders();
    Order getOrderByIdAdmin(String orderId);
    Order updateOrderStatus(String orderId, String status);
    List<Order> getDisputedOrders();
    Order resolveDispute(String orderId, String action); // action: "refund" or "release"
    Map<String, Object> getAdminStatistics(String timeframe);
}
