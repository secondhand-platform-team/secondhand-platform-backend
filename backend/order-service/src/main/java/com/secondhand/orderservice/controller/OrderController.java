package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.request.CreateOrderRequest;
import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.Shipment;
import com.secondhand.orderservice.security.JwtAuthenticatedUser;
import com.secondhand.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ==================== BUYER ENDPOINTS ====================

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/me")
    public ResponseEntity<List<Order>> getMyOrders(
            @AuthenticationPrincipal JwtAuthenticatedUser user) {
        return ResponseEntity.ok(orderService.getOrdersByBuyerId(user.userId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId, user.userId()));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user.userId()));
    }

    @PutMapping("/{orderId}/received")
    public ResponseEntity<Order> confirmReceived(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.confirmReceived(orderId, user.userId()));
    }

    @PutMapping("/{orderId}/dispute")
    public ResponseEntity<Order> disputeOrder(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        return ResponseEntity.ok(orderService.disputeOrder(orderId, user.userId(), reason));
    }

    // ==================== SELLER ENDPOINTS ====================

    @GetMapping("/seller")
    public ResponseEntity<List<Order>> getSellerOrders(
            @AuthenticationPrincipal JwtAuthenticatedUser user) {
        return ResponseEntity.ok(orderService.getOrdersBySellerId(user.userId()));
    }

    @PutMapping("/seller/{orderId}/preparing")
    public ResponseEntity<Order> confirmPreparing(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.confirmPreparing(orderId, user.userId()));
    }

    @PutMapping("/seller/{orderId}/handover")
    public ResponseEntity<Order> handoverToShipper(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId,
            @RequestBody Shipment shipmentData) {
        return ResponseEntity.ok(orderService.handoverToShipper(orderId, user.userId(), shipmentData));
    }

    @PutMapping("/seller/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrderBySeller(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.cancelOrderBySeller(orderId, user.userId()));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/admin/{orderId}")
    public ResponseEntity<Order> getOrderAdmin(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderByIdAdmin(orderId));
    }

    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }

    @GetMapping("/admin/disputes")
    public ResponseEntity<List<Order>> getDisputedOrders() {
        return ResponseEntity.ok(orderService.getDisputedOrders());
    }

    @PutMapping("/admin/{orderId}/resolve-dispute")
    public ResponseEntity<Order> resolveDispute(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body) {
        String action = body.get("action"); // "refund" or "release"
        return ResponseEntity.ok(orderService.resolveDispute(orderId, action));
    }

    @GetMapping("/admin/statistics")
    public ResponseEntity<Map<String, Object>> getAdminStatistics(
            @RequestParam(defaultValue = "month") String timeframe) {
        return ResponseEntity.ok(orderService.getAdminStatistics(timeframe));
    }

    // ==================== EVENT SOURCING ENDPOINTS ====================

    /**
     * Event Sourcing — Lấy timeline events của 1 order.
     * Trả về danh sách events theo thứ tự thời gian.
     * 
     * Dùng cho:
     * - UI hiển thị lịch sử đơn hàng chi tiết
     * - Debug production issues
     * - Audit trail
     */
    @GetMapping("/{orderId}/events")
    public ResponseEntity<?> getOrderEvents(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderEvents(orderId));
    }
}
