package com.secondhand.orderservice.model.enums;

public enum OrderStatus {

    PENDING_PAYMENT,      // vừa tạo đơn, chờ thanh toán

    PAID,                 // đã thanh toán (tiền vào escrow)

    PREPARING,            // seller đang chuẩn bị hàng

    HANDOVER_TO_SHIPPER,  // seller đã giao cho shipper

    IN_TRANSIT,           // đang vận chuyển (shipper giả lập)

    DELIVERED,            // đã giao hàng (shipper giả lập)

    RECEIVED,             // buyer xác nhận đã nhận

    COMPLETED,            // hoàn tất (tiền released cho seller)

    CANCELLED,            // đã hủy

    DISPUTED              // tranh chấp
}