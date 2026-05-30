package com.secondhand.coreservice.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body cho API gia hạn tin đăng.
 * Áp dụng cho tin SELL (tính phí); FREE_SELL/GIVE_AWAY luôn miễn phí.
 *
 * paymentMethod: "WALLET" hoặc "VNPAY" (default: "WALLET")
 */
@Getter
@Setter
@NoArgsConstructor
public class RenewRequest {

    /**
     * Phương thức thanh toán gia hạn.
     * - "WALLET": trừ phí từ ví (nếu đủ số dư)
     * - "VNPAY":  tạo link thanh toán VNPay, trả về paymentUrl
     */
    private String paymentMethod = "WALLET";
}
