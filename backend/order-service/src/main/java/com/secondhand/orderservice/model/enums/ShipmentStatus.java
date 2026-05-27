package com.secondhand.orderservice.model.enums;

public enum ShipmentStatus {

    PREPARING,        // seller đang đóng gói

    PICKED_UP,        // shipper đã lấy hàng

    IN_TRANSIT,       // đang vận chuyển

    DELIVERED,        // đã giao thành công

    FAILED_DELIVERY   // giao thất bại
}
