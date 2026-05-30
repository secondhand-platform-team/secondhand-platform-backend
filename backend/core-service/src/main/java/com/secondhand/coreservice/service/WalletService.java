package com.secondhand.coreservice.service;

import com.secondhand.coreservice.dto.request.DepositRequest;
import com.secondhand.coreservice.dto.request.VNPayCallbackRequest;
import com.secondhand.coreservice.dto.response.WalletResponse;
import com.secondhand.coreservice.dto.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface WalletService {
    WalletResponse getWalletBalance();

    Map<String, Object> createDepositPayment(DepositRequest request);

    // Giống ItemService.handleVNPayCallback()
    void handleVNPayCallback(VNPayCallbackRequest request);

    void deductFee(String userId, java.math.BigDecimal amount, String description);

    void addMoney(String userId, java.math.BigDecimal amount, String description);

    // Lịch sử giao dịch - lấy tất cả, sắp xếp mới nhất trước
    List<WalletTransactionResponse> getTransactionHistory();

    // Lịch sử giao dịch - có phân trang
    Page<WalletTransactionResponse> getTransactionHistoryPaged(int page, int size);

    // Lấy toàn bộ giao dịch hệ thống cho Admin
    Page<WalletTransactionResponse> getAllTransactionsForAdmin(int page, int size);

    // ====== Escrow Methods ======

    /** Tạm giữ tiền buyer khi checkout → escrow */
    void escrowHold(String buyerId, double amount, String orderId);

    /** Release tiền từ escrow → ví seller khi hoàn tất */
    void escrowRelease(String sellerId, double amount, String orderId);

    /** Hoàn tiền từ escrow → ví buyer khi cancel/dispute */
    void escrowRefund(String buyerId, double amount, String orderId);
}

