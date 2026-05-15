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

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, user.userId()));
    }

    @PutMapping("/{orderId}/return")
    public ResponseEntity<Order> returnOrder(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.returnOrder(orderId, user.userId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId, user.userId()));
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

    @PostMapping("/admin/{orderId}/shipment")
    public ResponseEntity<Order> createShipment(
            @PathVariable String orderId,
            @RequestBody Shipment shipment) {
        return ResponseEntity.ok(orderService.createShipment(orderId, shipment));
    }

    @PutMapping("/admin/{orderId}/shipment")
    public ResponseEntity<Order> updateShipment(
            @PathVariable String orderId,
            @RequestBody Shipment shipment) {
        return ResponseEntity.ok(orderService.updateShipment(orderId, shipment));
    }

    @GetMapping("/admin/statistics")
    public ResponseEntity<Map<String, Object>> getAdminStatistics(
            @RequestParam(defaultValue = "month") String timeframe) {
        return ResponseEntity.ok(orderService.getAdminStatistics(timeframe));
    }
}
