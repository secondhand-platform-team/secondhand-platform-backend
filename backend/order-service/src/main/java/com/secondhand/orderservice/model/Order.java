package com.secondhand.orderservice.model;

import com.secondhand.orderservice.model.enums.OrderStatus;
import com.secondhand.orderservice.model.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = {"orderItems", "payment", "shipment"})
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    // buyer
    private String buyerId;

    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    // snapshot địa chỉ giao hàng
    private String receiverName;

    private String receiverPhone;

    private String shippingAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 1 Order -> nhiều OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems;

    // 1 Order -> 1 Payment
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Payment payment;

    // 1 Order -> 1 Shipment
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Shipment shipment;

    @Transient
    private String paymentUrl;
}