package com.secondhand.orderservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {

    private String receiverName;

    private String receiverPhone;

    private String shippingAddress;

    private String paymentMethod; // WALLET, VNPAY

    private String itemId; // chỉ 1 item duy nhất (secondhand)
}
