package com.secondhand.orderservice.repository;

import com.secondhand.orderservice.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Event Sourcing — Repository cho OrderEvent
 * 
 * Chỉ cần đọc events theo orderId (order by createdAt ASC để replay đúng thứ tự).
 */
public interface OrderEventRepository extends JpaRepository<OrderEvent, String> {

    /** Lấy toàn bộ events của 1 order, sắp xếp theo thời gian tạo */
    List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
