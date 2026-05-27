package com.secondhand.coreservice.model.enums;

public enum ItemStatus {
    ACTIVE,     // đang bán
    RESERVED,   // đã có người checkout
    SOLD,       // giao thành công
    HIDDEN,     // ẩn bởi seller
    DRAFT,      // nháp
    EXPIRED,    // hết hạn
    CANCELLED   // order bị hủy → item quay lại ACTIVE (trạng thái tạm)
}
