package com.secondhand.coreservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secondhand.coreservice.model.enums.WalletTransactionStatus;
import com.secondhand.coreservice.model.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    @JsonIgnore
    private Wallet wallet;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    private WalletTransactionStatus status;

    private String referenceId; // Used to store VNPay TxnRef or Order ID

    private LocalDateTime createdAt;
}
