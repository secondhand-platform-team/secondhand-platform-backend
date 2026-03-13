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
@Table(name = "order_history")
public class OrderHistory {

    @Id
    private String id;

    private String orderId;

    private String oldStatus;

    private String newStatus;

    private LocalDateTime changedAt;
}