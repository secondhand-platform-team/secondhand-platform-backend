package com.secondhand.orderservice.service;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.Shipment;

/**
 * CQRS — Command Service Interface
 * 
 * Chứa tất cả Write operations (tạo, sửa, hủy, chuyển trạng thái).
 * Tách riêng khỏi Query để tuân theo Interface Segregation Principle.
 * 
 * Mỗi command sẽ tạo OrderEvent (Event Sourcing) để tracking lịch sử.
 */
public interface OrderCommandService {

    // ====== Buyer Commands ======
    Order createOrder(String buyerId, CreateOrderRequest request);
    Order cancelOrder(String orderId, String buyerId);
    Order confirmReceived(String orderId, String buyerId);
    Order disputeOrder(String orderId, String buyerId, String reason);

    // ====== Seller Commands ======
    Order confirmPreparing(String orderId, String sellerId);
    Order handoverToShipper(String orderId, String sellerId, Shipment shipmentData);
    Order cancelOrderBySeller(String orderId, String sellerId);

    // ====== Admin Commands ======
    Order updateOrderStatus(String orderId, String status);
    Order resolveDispute(String orderId, String action);
}
