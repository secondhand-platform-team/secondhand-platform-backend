package com.secondhand.orderservice.model.enums;

/**
 * Event Sourcing — Các loại event trong lifecycle đơn hàng.
 * 
 * Mỗi event đại diện cho 1 sự kiện đã xảy ra (immutable fact).
 * Chuỗi events có thể replay để rebuild lại state đơn hàng.
 */
public enum OrderEventType {

    // Order lifecycle events
    ORDER_CREATED,          // Đơn hàng được tạo
    ORDER_PAID,             // Thanh toán thành công (VNPay callback hoặc Wallet)
    ORDER_PREPARING,        // Seller bắt đầu chuẩn bị hàng
    ORDER_HANDOVER,         // Seller giao cho shipper
    ORDER_IN_TRANSIT,       // Đang vận chuyển
    ORDER_DELIVERED,        // Đã giao hàng
    ORDER_COMPLETED,        // Buyer xác nhận nhận hàng → hoàn tất
    ORDER_AUTO_COMPLETED,   // Hệ thống tự hoàn tất sau 3 ngày
    ORDER_CANCELLED,        // Đơn bị hủy (buyer hoặc seller)
    ORDER_DISPUTED,         // Buyer khiếu nại
    ORDER_DISPUTE_RESOLVED, // Admin xử lý dispute

    // Escrow events (tài chính)
    ESCROW_HELD,            // Tiền buyer bị tạm giữ
    ESCROW_RELEASED,        // Tiền released cho seller
    ESCROW_REFUNDED,        // Tiền hoàn cho buyer

    // Status update (admin)
    STATUS_UPDATED          // Admin cập nhật trạng thái thủ công
}
