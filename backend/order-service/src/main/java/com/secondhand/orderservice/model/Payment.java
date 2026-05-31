package com.secondhand.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secondhand.orderservice.model.enums.PaymentMethod;
import com.secondhand.orderservice.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "order")
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private String id;

    private String transactionId;

    private String vnpTxnRef;

    private Double amount;

    private String responseCode;

    private String secureHash;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;

    @OneToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL)
    private Transaction transaction;
}