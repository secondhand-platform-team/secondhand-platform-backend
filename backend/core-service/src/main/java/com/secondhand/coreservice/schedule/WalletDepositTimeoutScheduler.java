package com.secondhand.coreservice.schedule;

import com.secondhand.coreservice.model.WalletTransaction;
import com.secondhand.coreservice.model.enums.NotificationType;
import com.secondhand.coreservice.model.enums.WalletTransactionStatus;
import com.secondhand.coreservice.model.enums.WalletTransactionType;
import com.secondhand.coreservice.repository.WalletTransactionRepository;
import com.secondhand.coreservice.service.NotificationService;
import com.secondhand.coreservice.service.PaymentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự động đánh dấu các giao dịch nạp tiền VNPay quá 2 phút là FAILED.
 *
 * Nếu VNPay không callback về hoặc user thoát giữa chừng,
 * transaction sẽ không bị treo ở PENDING mãi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletDepositTimeoutScheduler {

    private static final int DEPOSIT_TIMEOUT_MINUTES = 15;

    private final WalletTransactionRepository walletTransactionRepository;
    private final NotificationService notificationService;
    private final PaymentEventService paymentEventService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void markExpiredDepositsAsFailed() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(DEPOSIT_TIMEOUT_MINUTES);
        List<WalletTransaction> expiredDeposits = walletTransactionRepository
                .findByTypeAndStatusAndCreatedAtBefore(
                        WalletTransactionType.DEPOSIT,
                        WalletTransactionStatus.PENDING,
                        cutoff);

        if (expiredDeposits.isEmpty()) {
            return;
        }

        for (WalletTransaction tx : expiredDeposits) {
            try {
                if (tx.getStatus() != WalletTransactionStatus.PENDING) {
                    continue;
                }

                tx.setStatus(WalletTransactionStatus.FAILED);
                walletTransactionRepository.save(tx);

                try {
                    paymentEventService.updatePaymentStatus(tx.getReferenceId(), "FAILED");
                    log.info("[WalletDepositTimeout] Payment status updated to FAILED: {}", tx.getReferenceId());
                } catch (Exception ex) {
                    log.warn("[WalletDepositTimeout] Failed to update payment status in order-service for {}: {}",
                            tx.getReferenceId(), ex.getMessage(), ex);
                }

                if (tx.getWallet() != null && tx.getWallet().getUserId() != null) {
                    notificationService.createAndSendNotification(
                            tx.getWallet().getUserId(),
                            "Giao dịch nạp tiền #" + tx.getReferenceId() + " đã hết hạn sau 15 phút.",
                            NotificationType.SYSTEM,
                            null
                    );
                }

                log.info("[WalletDepositTimeout] Marked deposit transaction as FAILED: {}", tx.getReferenceId());
            } catch (Exception e) {
                log.error("[WalletDepositTimeout] Failed to mark deposit transaction {} as failed: {}",
                        tx.getReferenceId(), e.getMessage(), e);
            }
        }
    }
}