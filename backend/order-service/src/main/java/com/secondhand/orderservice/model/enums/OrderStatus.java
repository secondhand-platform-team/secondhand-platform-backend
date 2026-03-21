package com.secondhand.orderservice.model.enums;

public enum OrderStatus {

    PENDING,      // vừa tạo đơn

    CONFIRMED,    // seller xác nhận đơn

    PAID,         // đã thanh toán

    SHIPPING,     // đang giao

    DELIVERED,    // giao thành công

    CANCELLED,    // hủy đơn

    RETURNED      // trả hàng

}