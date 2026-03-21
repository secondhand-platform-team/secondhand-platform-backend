package com.secondhand.orderservice.repository;

import com.secondhand.orderservice.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    boolean existsByUserId(String userId);

    Optional<Cart> findByUserId(String userId);
}