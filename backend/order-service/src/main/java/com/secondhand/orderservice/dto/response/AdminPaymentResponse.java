package com.secondhand.orderservice.dto.response;

import com.secondhand.orderservice.model.enums.PaymentMethod;
import com.secondhand.orderservice.model.enums.PaymentStatus;
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
public class AdminPaymentResponse {
    private String id;
    private String transactionId;
    private Double amount;
    private String responseCode;
    private PaymentMethod method;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    
    // Order info if available
    private String orderId;
    private String buyerId;
}
