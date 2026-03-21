package com.secondhand.orderservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    private String id;

    private String carrier;       // đơn vị vận chuyển (GHN, GHTK...)

    private String trackingCode;  // mã vận đơn

    private String status;        // SHIPPING / DELIVERED

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
}