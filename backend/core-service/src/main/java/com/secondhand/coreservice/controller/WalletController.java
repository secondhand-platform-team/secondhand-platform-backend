package com.secondhand.coreservice.controller;

import java.net.URI;

import com.secondhand.coreservice.dto.request.DepositRequest;
import com.secondhand.coreservice.dto.request.VNPayCallbackRequest;
import com.secondhand.coreservice.dto.response.WalletResponse;
import com.secondhand.coreservice.dto.response.WalletTransactionResponse;
import com.secondhand.coreservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor

public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getWalletBalance() {
        WalletResponse response = walletService.getWalletBalance();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(
            @Valid @RequestBody DepositRequest request) {

        Map<String, Object> response = walletService.createDepositPayment(request);

        if ("00".equals(response.get("code"))) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Copy y hệt pattern từ ItemController.handleVNPayCallback()
    @GetMapping("/payment-callback")
    public ResponseEntity<?> handleVNPayCallback(
            @RequestParam(required = false) String vnp_Amount,
            @RequestParam(required = false) String vnp_BankCode,
            @RequestParam(required = false) String vnp_BankTranNo,
            @RequestParam(required = false) String vnp_CardType,
            @RequestParam(required = false) String vnp_OrderInfo,
            @RequestParam(required = false) String vnp_PayDate,
            @RequestParam(required = false) String vnp_ResponseCode,
            @RequestParam(required = false) String vnp_TmnCode,
            @RequestParam(required = false) String vnp_TransactionNo,
            @RequestParam(required = false) String vnp_TransactionStatus,
            @RequestParam(required = false) String vnp_TxnRef,
            @RequestParam(required = false) String vnp_SecureHash) {
        try {
            VNPayCallbackRequest request = VNPayCallbackRequest.builder()
                    .vnp_Amount(vnp_Amount)
                    .vnp_BankCode(vnp_BankCode)
                    .vnp_BankTranNo(vnp_BankTranNo)
                    .vnp_CardType(vnp_CardType)
                    .vnp_OrderInfo(vnp_OrderInfo)
                    .vnp_PayDate(vnp_PayDate)
                    .vnp_ResponseCode(vnp_ResponseCode)
                    .vnp_TmnCode(vnp_TmnCode)
                    .vnp_TransactionNo(vnp_TransactionNo)
                    .vnp_TransactionStatus(vnp_TransactionStatus)
                    .vnp_TxnRef(vnp_TxnRef)
                    .vnp_SecureHash(vnp_SecureHash)
                    .build();

            walletService.handleVNPayCallback(request);

            // Redirect to frontend success page
            String successUrl = "http://localhost:3000/payment-success?status=success&transactionId="
                    + vnp_TransactionNo;
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(successUrl))
                    .build();
        } catch (Exception e) {
            // Redirect to frontend error page
            String errorUrl = "http://localhost:3000/payment-failed?status=error&message=" +
                    java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
    }

    /**
     * Lấy toàn bộ lịch sử giao dịch của ví hiện tại, sắp xếp mới nhất trước.
     * GET /api/wallet/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactionHistory() {
        List<WalletTransactionResponse> transactions = walletService.getTransactionHistory();
        return ResponseEntity.ok(transactions);
    }

    /**
     * Lấy lịch sử giao dịch có phân trang.
     * GET /api/wallet/transactions/paged?page=0&size=10
     */
    @GetMapping("/transactions/paged")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactionHistoryPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WalletTransactionResponse> result = walletService.getTransactionHistoryPaged(page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * API Nội bộ: Trừ tiền ví (gọi từ order-service)
     * POST /api/wallet/internal/deduct
     */
    @PostMapping("/internal/deduct")
    public ResponseEntity<Void> deductInternal(@RequestBody java.util.Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Object amountObj = request.get("amount");
        java.math.BigDecimal amount;
        if (amountObj instanceof Number) {
            amount = java.math.BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else {
            amount = new java.math.BigDecimal(amountObj.toString());
        }
        String description = (String) request.get("description");
        walletService.deductFee(userId, amount, description);
        return ResponseEntity.ok().build();
    }

    /**
     * API Nội bộ: Cộng tiền ví (gọi từ order-service)
     * POST /api/wallet/internal/add
     */
    @PostMapping("/internal/add")
    public ResponseEntity<Void> addInternal(@RequestBody java.util.Map<String, Object> request) {
        String userId = (String) request.get("userId");
        Object amountObj = request.get("amount");
        java.math.BigDecimal amount;
        if (amountObj instanceof Number) {
            amount = java.math.BigDecimal.valueOf(((Number) amountObj).doubleValue());
        } else {
            amount = new java.math.BigDecimal(amountObj.toString());
        }
        String description = (String) request.get("description");
        walletService.addMoney(userId, amount, description);
        return ResponseEntity.ok().build();
    }
}
