package com.secondhand.orderservice.repository;

import com.secondhand.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    Optional<Order> findByIdAndBuyerId(String id, String buyerId);

    List<Order> findAllByOrderByCreatedAtDesc();

    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate")
    Double getTotalRevenue(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate")
    Long getTotalOrders(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT CAST(created_at AS DATE) as date, SUM(total_price) as revenue FROM orders WHERE status != 'CANCELLED' AND created_at >= :startDate GROUP BY CAST(created_at AS DATE) ORDER BY date", nativeQuery = true)
    List<Object[]> getRevenueByTimeframe(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT CAST(created_at AS DATE) as date, COUNT(*) as count FROM orders WHERE created_at >= :startDate GROUP BY CAST(created_at AS DATE) ORDER BY date", nativeQuery = true)
    List<Object[]> getOrdersByTimeframe(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT oi.seller_id, SUM(oi.price * oi.quantity) as revenue, COUNT(DISTINCT oi.order_id) as orders " +
                   "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                   "WHERE o.created_at >= :startDate GROUP BY oi.seller_id ORDER BY revenue DESC LIMIT 5", nativeQuery = true)
    List<Object[]> getTopSellersByTimeframe(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT oi.item_id, oi.item_name, SUM(oi.quantity) as sales " +
                   "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                   "WHERE o.created_at >= :startDate GROUP BY oi.item_id, oi.item_name ORDER BY sales DESC LIMIT 5", nativeQuery = true)
    List<Object[]> getTopProductsByTimeframe(@Param("startDate") LocalDateTime startDate);
}
