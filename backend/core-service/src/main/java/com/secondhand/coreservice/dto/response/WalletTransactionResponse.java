package com.secondhand.coreservice.dto.response;

import com.secondhand.coreservice.model.enums.WalletTransactionStatus;
import com.secondhand.coreservice.model.enums.WalletTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class WalletTransactionResponse {
    private String id;
    private Double amount;
    private WalletTransactionType type;
    private WalletTransactionStatus status;
    private String referenceId;
    private LocalDateTime createdAt;
}
