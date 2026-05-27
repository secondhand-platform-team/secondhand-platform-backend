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
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_buyer_id", columnList = "buyerId"),
    @Index(name = "idx_orders_seller_id", columnList = "sellerId"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_created_at", columnList = "createdAt")
})
public class Order {

    @Id
    private String id;

    // buyer
    private String buyerId;

    // seller (1 order = 1 item = 1 seller trong secondhand)
    private String sellerId;

    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    // snapshot địa chỉ giao hàng
    private String receiverName;

    private String receiverPhone;

    private String shippingAddress;

    // Escrow transaction tracking
    private String escrowTransactionId;

    // Auto-complete deadline (3 ngày sau khi DELIVERED)
    private LocalDateTime autoCompleteAt;

    // Lý do hủy đơn
    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    // Lý do tranh chấp
    @Column(columnDefinition = "TEXT")
    private String disputeReason;

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