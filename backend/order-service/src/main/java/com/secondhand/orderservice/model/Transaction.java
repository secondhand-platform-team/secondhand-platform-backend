package com.secondhand.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secondhand.orderservice.model.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "payment")
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private String id;

    private String transactionCode;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private LocalDateTime createdAt;

    @OneToOne
    @JoinColumn(name = "payment_id")
    @JsonIgnore
    private Payment payment;
}