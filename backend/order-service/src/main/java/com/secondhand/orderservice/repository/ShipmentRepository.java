package com.secondhand.orderservice.repository;

import com.secondhand.orderservice.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {

    Optional<Shipment> findByOrderId(String orderId);
}
