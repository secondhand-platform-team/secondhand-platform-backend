package com.secondhand.orderservice.repository;

import com.secondhand.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(String buyerId);

    Optional<Order> findByIdAndBuyerId(String id, String buyerId);

    List<Order> findAllByOrderByCreatedAtDesc();
}
