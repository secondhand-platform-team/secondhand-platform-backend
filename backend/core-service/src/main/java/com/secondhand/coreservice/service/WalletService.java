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

    // Lịch sử giao dịch - lấy tất cả, sắp xếp mới nhất trước
    List<WalletTransactionResponse> getTransactionHistory();

    // Lịch sử giao dịch - có phân trang
    Page<WalletTransactionResponse> getTransactionHistoryPaged(int page, int size);
}

