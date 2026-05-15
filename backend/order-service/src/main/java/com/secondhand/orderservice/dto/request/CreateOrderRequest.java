package com.secondhand.orderservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {

    private String receiverName;

    private String receiverPhone;

    private String shippingAddress;

    private String paymentMethod; // COD, BANK_TRANSFER, MOMO, VNPAY

    private List<OrderItemRequest> items;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemRequest {
        private String itemId;
        private String itemName;
        private String sellerId;
        private Double price;
        private Integer quantity;
    }
}
