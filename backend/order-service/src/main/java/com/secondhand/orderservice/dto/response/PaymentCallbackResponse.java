package com.secondhand.orderservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentCallbackResponse {
    private String orderId;
    private String transactionId;
    private String amount;
    private String responseCode;
    private String message;
    private String bankCode;
    private String bankTranNo;
    private String cardType;
}
