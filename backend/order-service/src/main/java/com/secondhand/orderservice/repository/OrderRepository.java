package com.secondhand.orderservice.repository;

import com.secondhand.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    Optional<Order> findByIdAndBuyerId(String id, String buyerId);

    List<Order> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status != 'CANCELLED'")
    Double getTotalRevenue();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o")
    Long getTotalOrders();

    @org.springframework.data.jpa.repository.Query(value = "SELECT CAST(created_at AS DATE) as date, SUM(total_price) as revenue FROM orders WHERE status != 'CANCELLED' AND created_at >= CURRENT_DATE - INTERVAL '7 days' GROUP BY CAST(created_at AS DATE) ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyRevenueLast7Days();

    @org.springframework.data.jpa.repository.Query(value = "SELECT CAST(created_at AS DATE) as date, COUNT(*) as count FROM orders WHERE created_at >= CURRENT_DATE - INTERVAL '7 days' GROUP BY CAST(created_at AS DATE) ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyOrdersLast7Days();

    @org.springframework.data.jpa.repository.Query(value = "SELECT seller_id, SUM(price * quantity) as revenue, COUNT(DISTINCT order_id) as orders FROM order_items GROUP BY seller_id ORDER BY revenue DESC LIMIT 5", nativeQuery = true)
    List<Object[]> getTopSellers();

    @org.springframework.data.jpa.repository.Query(value = "SELECT item_id, item_name, SUM(quantity) as sales FROM order_items GROUP BY item_id, item_name ORDER BY sales DESC LIMIT 5", nativeQuery = true)
    List<Object[]> getTopProducts();
}
