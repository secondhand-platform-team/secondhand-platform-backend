package com.secondhand.orderservice.repository;

import com.secondhand.orderservice.model.Order;
import com.secondhand.orderservice.model.enums.OrderStatus;
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

    List<Order> findBySellerIdAndStatusNotOrderByCreatedAtDesc(String sellerId, OrderStatus status);

    List<Order> findByStatusOrderByUpdatedAtDesc(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING_PAYMENT' AND o.createdAt <= :before")
    List<Order> findPendingPaymentOrdersBefore(@Param("before") LocalDateTime before);

    // Tìm orders cần giả lập IN_TRANSIT (HANDOVER_TO_SHIPPER quá X giây)
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.updatedAt <= :before")
    List<Order> findByStatusAndUpdatedAtBefore(
            @Param("status") OrderStatus status,
            @Param("before") LocalDateTime before);

    // Tìm orders cần auto-complete (DELIVERED quá deadline)
    @Query("SELECT o FROM Order o WHERE o.status = 'DELIVERED' AND o.autoCompleteAt IS NOT NULL AND o.autoCompleteAt <= :now")
    List<Order> findOrdersToAutoComplete(@Param("now") LocalDateTime now);

    // Statistics
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status != 'CANCELLED' AND o.createdAt >= :startDate")
    Double getTotalRevenue(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate")
    Long getTotalOrders(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT CAST(created_at AS DATE) as date, SUM(total_price) as revenue FROM orders WHERE status != 'CANCELLED' AND created_at >= :startDate GROUP BY CAST(created_at AS DATE) ORDER BY date", nativeQuery = true)
    List<Object[]> getRevenueByTimeframe(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT CAST(created_at AS DATE) as date, COUNT(*) as count FROM orders WHERE created_at >= :startDate GROUP BY CAST(created_at AS DATE) ORDER BY date", nativeQuery = true)
    List<Object[]> getOrdersByTimeframe(@Param("startDate") LocalDateTime startDate);
}
