package com.secondhand.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secondhand.orderservice.model.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "order")
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    private String id;

    private String carrier;       // đơn vị vận chuyển (GHN, GHTK...)

    private String trackingCode;  // mã vận đơn

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    @Column(columnDefinition = "TEXT")
    private String currentLocation;      // vị trí hiện tại (giả lập)

    private LocalDateTime estimatedDelivery;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @OneToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;
}