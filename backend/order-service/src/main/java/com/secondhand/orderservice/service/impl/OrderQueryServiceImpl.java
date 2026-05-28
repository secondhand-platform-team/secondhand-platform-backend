package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.OrderEvent;
import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.repository.OrderRepository;
import com.secondhand.orderservice.service.OrderEventStore;
import com.secondhand.orderservice.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CQRS — Query Service Implementation
 * 
 * Chỉ chứa Read operations. Tất cả query được đánh dấu @Transactional(readOnly = true)
 * để PostgreSQL optimize cho read:
 * - Không acquire write lock
 * - Có thể route sang read replica (nếu có)
 * - Connection pool optimize
 */
@Service("orderQueryService")
@RequiredArgsConstructor
@Slf4j
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderEventStore eventStore;

    // ====================================================================
    // BUYER Queries
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByBuyerId(String buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(String orderId, String buyerId) {
        return orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
    }

    // ====================================================================
    // SELLER Queries
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersBySellerId(String sellerId) {
        return orderRepository.findBySellerIdAndStatusNotOrderByCreatedAtDesc(sellerId, OrderStatus.PENDING_PAYMENT);
    }

    // ====================================================================
    // ADMIN Queries
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderByIdAdmin(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getDisputedOrders() {
        return orderRepository.findByStatusOrderByUpdatedAtDesc(
                com.secondhand.orderservice.model.enums.OrderStatus.DISPUTED);
    }

    @Override
    @Transactional(readOnly = true)
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
    // Event Sourcing Queries
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderEvent> getOrderEvents(String orderId) {
        return eventStore.getEventsByOrderId(orderId);
    }
}
