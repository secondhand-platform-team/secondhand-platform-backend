package com.secondhand.coreservice.model.enums;

public enum NotificationType {

    ITEM_FAVORITED,
    ITEM_COMMENTED,
    ITEM_REPORTED,
    GIVEAWAY_REQUEST,
    SYSTEM,
    WALLET_DEPOSIT_SUCCESS,
    WALLET_DEDUCTION,

    // Order flow notifications
    ORDER_CREATED,          // buyer đặt hàng thành công
    ORDER_NEW_FOR_SELLER,   // seller nhận đơn mới
    ORDER_PREPARING,        // seller đang chuẩn bị hàng → notify buyer
    ORDER_HANDOVER,         // seller giao cho shipper → notify buyer
    ORDER_IN_TRANSIT,       // shipper đang giao → notify cả 2
    ORDER_DELIVERED,        // shipper đã giao → notify cả 2
    ORDER_RECEIVED,         // buyer xác nhận nhận → notify seller
    ORDER_COMPLETED,        // hoàn tất + tiền released → notify cả 2
    ORDER_CANCELLED,        // hủy đơn → notify bên kia
    ORDER_DISPUTED,         // buyer khiếu nại → notify seller + admin
    ORDER_DISPUTE_RESOLVED, // admin xử lý dispute → notify cả 2
    ORDER_AUTO_COMPLETED,   // auto-complete timeout → notify cả 2
    ORDER_STATUS,           // generic status update (backward compat)

    // Escrow notifications
    ESCROW_HOLD,            // tiền buyer bị hold
    ESCROW_RELEASED,        // tiền released cho seller
    ESCROW_REFUNDED         // tiền hoàn cho buyer
}