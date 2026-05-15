package com.secondhand.orderservice.service;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.Shipment;

import java.util.List;

public interface OrderService {

    Order createOrder(String buyerId, CreateOrderRequest request);

    List<Order> getOrdersByBuyerId(String buyerId);

    Order getOrderById(String orderId, String buyerId);

    Order cancelOrder(String orderId, String buyerId);

    Order returnOrder(String orderId, String buyerId);

    // Admin
    List<Order> getAllOrders();

    Order getOrderByIdAdmin(String orderId);

    Order updateOrderStatus(String orderId, String status);

    Order createShipment(String orderId, Shipment shipment);

    Order updateShipment(String orderId, Shipment shipment);
    java.util.Map<String, Object> getAdminStatistics(String timeframe);
}
