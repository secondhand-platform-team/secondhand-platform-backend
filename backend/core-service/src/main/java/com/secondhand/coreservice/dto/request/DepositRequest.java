package com.secondhand.coreservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DepositRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Long amount;

    private String bankCode;

    private String language = "vn";
}
