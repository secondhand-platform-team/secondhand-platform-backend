package com.secondhand.coreservice.service.impl;

import com.secondhand.coreservice.client.PaymentRestClient.PaymentCreateResult;
import com.secondhand.coreservice.dto.request.DepositRequest;
import com.secondhand.coreservice.dto.request.VNPayCallbackRequest;
import com.secondhand.coreservice.dto.response.WalletResponse;
import com.secondhand.coreservice.dto.response.WalletTransactionResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.exception.UnauthorizedException;
import com.secondhand.coreservice.model.Wallet;
import com.secondhand.coreservice.model.WalletTransaction;
import com.secondhand.coreservice.model.enums.WalletTransactionStatus;
import com.secondhand.coreservice.model.enums.WalletTransactionType;
import com.secondhand.coreservice.repository.WalletRepository;
import com.secondhand.coreservice.repository.WalletTransactionRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.PaymentEventService;
import com.secondhand.coreservice.service.NotificationService;
import com.secondhand.coreservice.model.enums.NotificationType;
import com.secondhand.coreservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    // Giống ItemServiceImpl dùng PaymentEventService
    private final PaymentEventService paymentEventService;
    private final NotificationService notificationService;

    @Value("${app.payment.wallet-callback-url}")
    private String walletCallbackUrl;

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticatedUser) {
            JwtAuthenticatedUser user = (JwtAuthenticatedUser) authentication.getPrincipal();
            return user.userId();
        }
        throw new UnauthorizedException("Unauthorized");
    }

    @Override
    public WalletResponse getWalletBalance() {
        String userId = getCurrentUserId();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createNewWallet(userId));
        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getBalance());
    }

    private Wallet createNewWallet(String userId) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID().toString());
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        wallet.setCreatedAt(LocalDateTime.now());
        return walletRepository.save(wallet);
    }

    // ====================================================================
    // Giống ItemServiceImpl.createItem() phần gọi paymentEventService
    // ====================================================================
    @Override
    public Map<String, Object> createDepositPayment(DepositRequest request) {
        try {
            String userId = getCurrentUserId();

            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseGet(() -> createNewWallet(userId));

            log.info("Creating deposit payment for userId={}, amount={}", userId, request.getAmount());

            // Giống ItemServiceImpl: gọi paymentEventService.createVnPayPayment()
            // Truyền custom returnUrl để VNPay redirect về wallet callback (qua Kong)
            PaymentCreateResult paymentResponse = paymentEventService.createVnPayPayment(
                    request.getAmount(),
                    request.getBankCode(),
                    request.getLanguage(),
                    userId,
                    walletCallbackUrl
            );

            if ("00".equals(paymentResponse.code())) {
                // Giống ItemServiceImpl: lưu transactionId vào entity
                WalletTransaction wTx = new WalletTransaction();
                wTx.setId(UUID.randomUUID().toString());
                wTx.setWallet(wallet);
                wTx.setAmount((double) request.getAmount());
                wTx.setType(WalletTransactionType.DEPOSIT);
                wTx.setStatus(WalletTransactionStatus.PENDING);
                wTx.setReferenceId(paymentResponse.transactionId());
                wTx.setCreatedAt(LocalDateTime.now());
                walletTransactionRepository.save(wTx);

                log.info("Deposit initiated - transactionId={}", paymentResponse.transactionId());

                return Map.of(
                        "code", "00",
                        "message", "success",
                        "paymentUrl", paymentResponse.paymentUrl(),
                        "transactionId", paymentResponse.transactionId()
                );
            } else {
                log.error("Failed to create deposit payment: {}", paymentResponse.message());
                return Map.of("code", "99", "message", paymentResponse.message());
            }

        } catch (Exception e) {
            if (e instanceof UnauthorizedException) {
                throw (UnauthorizedException) e;
            }
            log.error("Error creating deposit payment", e);
            return Map.of("code", "99", "message", "error: " + e.getMessage());
        }
    }

    // ====================================================================
    // Copy y hệt ItemServiceImpl.handleVNPayCallback()
    // Chỉ thay: Item → WalletTransaction, DRAFT→ACTIVE → PENDING→SUCCESS + cộng tiền
    // ====================================================================
    @Override
    @Transactional
    public void handleVNPayCallback(VNPayCallbackRequest request) {
        try {
            log.info("Processing VNPay wallet callback - TxnRef: {}, ResponseCode: {}",
                    request.getVnp_TxnRef(), request.getVnp_ResponseCode());

            // Verify response code (00 = success)
            if (!"00".equals(request.getVnp_ResponseCode())) {
                log.warn("VNPay callback with non-success response code: {}", request.getVnp_ResponseCode());

                                List<WalletTransaction> transactions = walletTransactionRepository.findAll();
                                WalletTransaction targetTx = transactions.stream()
                                                .filter(tx -> tx.getReferenceId() != null &&
                                                                tx.getReferenceId().contains(request.getVnp_TxnRef()))
                                                .findFirst()
                                                .orElse(null);

                                if (targetTx != null && targetTx.getStatus() == WalletTransactionStatus.PENDING) {
                                        targetTx.setStatus(WalletTransactionStatus.FAILED);
                                        walletTransactionRepository.save(targetTx);

                                        try {
                                                paymentEventService.updatePaymentStatus(targetTx.getReferenceId(), "FAILED");
                                                log.info("Payment status updated to FAILED for transactionId: {}", targetTx.getReferenceId());
                                        } catch (Exception ex) {
                                                log.warn("Failed to update payment status in order-service", ex);
                                        }
                                }
                return;
            }

            // Find WalletTransaction by transactionId (which contains TxnRef)
            // Giống ItemServiceImpl: tìm item bằng transactionId.contains(TxnRef)
            List<WalletTransaction> transactions = walletTransactionRepository.findAll();
            WalletTransaction targetTx = transactions.stream()
                    .filter(tx -> tx.getReferenceId() != null &&
                            tx.getReferenceId().contains(request.getVnp_TxnRef()))
                    .findFirst()
                    .orElse(null);

            if (targetTx == null) {
                log.warn("No WalletTransaction found with TxnRef: {}", request.getVnp_TxnRef());
                return;
            }

            // Update transaction status: PENDING → SUCCESS (giống Item: DRAFT → ACTIVE)
            targetTx.setStatus(WalletTransactionStatus.SUCCESS);
            walletTransactionRepository.save(targetTx);

            // Cộng tiền vào ví
            Wallet wallet = targetTx.getWallet();
            wallet.setBalance(wallet.getBalance() + targetTx.getAmount());
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);

            // Update payment status in order-service (giống ItemServiceImpl)
            try {
                paymentEventService.updatePaymentStatus(targetTx.getReferenceId(), "PAID");
                log.info("Payment status updated to PAID for transactionId: {}", targetTx.getReferenceId());
            } catch (Exception ex) {
                log.warn("Failed to update payment status in order-service", ex);
            }

            log.info("Wallet deposit SUCCESS - userId={}, amount={}, new balance={}",
                    wallet.getUserId(), targetTx.getAmount(), wallet.getBalance());

            notificationService.createAndSendNotification(
                    wallet.getUserId(),
                    "Nạp tiền thành công " + targetTx.getAmount() + " VNĐ vào ví",
                    NotificationType.WALLET_DEPOSIT_SUCCESS,
                    null
            );
        } catch (Exception e) {
            log.error("Error processing VNPay wallet callback", e);
            throw new RuntimeException("Failed to process wallet payment callback: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deductFee(String userId, java.math.BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Wallet not found for user: " + userId));

        if (wallet.getBalance() < amount.longValue()) {
            throw new BadRequestException("Insufficient wallet balance. Please deposit money first.");
        }

        // Deduct balance
        wallet.setBalance(wallet.getBalance() - amount.longValue());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction tx = new WalletTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setWallet(wallet);
        tx.setAmount((double) amount.longValue());
        tx.setType(WalletTransactionType.PAYMENT);
        tx.setStatus(WalletTransactionStatus.SUCCESS);
//        tx.setDescription(description != null ? description : "Thanh toán phí đăng tin");
        tx.setReferenceId("FEE-" + System.currentTimeMillis());
        tx.setCreatedAt(LocalDateTime.now());
//        tx.setUpdatedAt(LocalDateTime.now());

        walletTransactionRepository.save(tx);
        log.info("Deducted {} from wallet of user {} for {}", amount, userId, description);

        notificationService.createAndSendNotification(
                userId,
                "Đã trừ " + amount.longValue() + " VNĐ từ ví: " + description,
                NotificationType.WALLET_DEDUCTION,
                null
        );
    }

    @Override
    @Transactional
    public void addMoney(String userId, java.math.BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createNewWallet(userId));

        // Add balance
        wallet.setBalance(wallet.getBalance() + amount.longValue());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction tx = new WalletTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setWallet(wallet);
        tx.setAmount((double) amount.longValue());
        tx.setType(WalletTransactionType.DEPOSIT);
        tx.setStatus(WalletTransactionStatus.SUCCESS);
        tx.setReferenceId("EARN-" + System.currentTimeMillis());
        tx.setCreatedAt(LocalDateTime.now());

        walletTransactionRepository.save(tx);
        log.info("Added {} to wallet of user {} for {}", amount, userId, description);

        notificationService.createAndSendNotification(
                userId,
                "Ví của bạn đã được cộng " + amount.longValue() + " VNĐ: " + description,
                NotificationType.WALLET_DEPOSIT_SUCCESS,
                null
        );
    }

    // ====================================================================
    // Lịch sử giao dịch
    // ====================================================================

    @Override
    public List<WalletTransactionResponse> getTransactionHistory() {
        String userId = getCurrentUserId();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createNewWallet(userId));

        List<WalletTransaction> transactions =
                walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<WalletTransactionResponse> getTransactionHistoryPaged(int page, int size) {
        String userId = getCurrentUserId();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> createNewWallet(userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WalletTransaction> transactionPage =
                walletTransactionRepository.findByWalletId(wallet.getId(), pageable);

        return transactionPage.map(this::mapToTransactionResponse);
    }

    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .referenceId(tx.getReferenceId())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    // ====================================================================
    // Escrow Methods — P2P Secondhand Marketplace
    // ====================================================================

    @Override
    @Transactional
    public void escrowHold(String buyerId, double amount, String orderId) {
        Wallet wallet = walletRepository.findByUserId(buyerId)
                .orElseThrow(() -> new BadRequestException("Wallet not found for user: " + buyerId));

        if (wallet.getBalance() < amount) {
            throw new BadRequestException("Số dư ví không đủ. Vui lòng nạp thêm tiền.");
        }

        // Trừ tiền buyer
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Tạo transaction record ESCROW_HOLD
        WalletTransaction tx = new WalletTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(WalletTransactionType.ESCROW_HOLD);
        tx.setStatus(WalletTransactionStatus.PENDING);
        tx.setReferenceId("ESCROW-HOLD-" + orderId);
        tx.setCreatedAt(LocalDateTime.now());
        walletTransactionRepository.save(tx);

        log.info("Escrow HOLD - buyerId={}, amount={}, orderId={}, new balance={}",
                buyerId, amount, orderId, wallet.getBalance());

        notificationService.createAndSendNotification(
                buyerId,
                "Ví đã tạm giữ " + (long) amount + " VNĐ cho đơn hàng #" + orderId.substring(0, 8).toUpperCase(),
                NotificationType.ESCROW_HOLD,
                null
        );
    }

    @Override
    @Transactional
    public void escrowRelease(String sellerId, double amount, String orderId) {
        Wallet wallet = walletRepository.findByUserId(sellerId)
                .orElseGet(() -> createNewWallet(sellerId));

        // Cộng tiền cho seller
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Tạo transaction record ESCROW_RELEASE
        WalletTransaction tx = new WalletTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(WalletTransactionType.ESCROW_RELEASE);
        tx.setStatus(WalletTransactionStatus.SUCCESS);
        tx.setReferenceId("ESCROW-RELEASE-" + orderId);
        tx.setCreatedAt(LocalDateTime.now());
        walletTransactionRepository.save(tx);

        log.info("Escrow RELEASE - sellerId={}, amount={}, orderId={}, new balance={}",
                sellerId, amount, orderId, wallet.getBalance());

        notificationService.createAndSendNotification(
                sellerId,
                "Ví được cộng " + (long) amount + " VNĐ từ đơn hàng #" + orderId.substring(0, 8).toUpperCase(),
                NotificationType.ESCROW_RELEASED,
                null
        );
    }

    @Override
    @Transactional
    public void escrowRefund(String buyerId, double amount, String orderId) {
        Wallet wallet = walletRepository.findByUserId(buyerId)
                .orElseThrow(() -> new BadRequestException("Wallet not found for user: " + buyerId));

        // Hoàn tiền cho buyer
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Tạo transaction record ESCROW_REFUND
        WalletTransaction tx = new WalletTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setWallet(wallet);
        tx.setAmount(amount);
        tx.setType(WalletTransactionType.ESCROW_REFUND);
        tx.setStatus(WalletTransactionStatus.SUCCESS);
        tx.setReferenceId("ESCROW-REFUND-" + orderId);
        tx.setCreatedAt(LocalDateTime.now());
        walletTransactionRepository.save(tx);

        log.info("Escrow REFUND - buyerId={}, amount={}, orderId={}, new balance={}",
                buyerId, amount, orderId, wallet.getBalance());

        notificationService.createAndSendNotification(
                buyerId,
                "Ví được hoàn " + (long) amount + " VNĐ từ đơn hàng #" + orderId.substring(0, 8).toUpperCase(),
                NotificationType.ESCROW_REFUNDED,
                null
        );
    }
}

