package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.WalletTransaction;
import com.secondhand.coreservice.model.enums.WalletTransactionStatus;
import com.secondhand.coreservice.model.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
    Optional<WalletTransaction> findByReferenceId(String referenceId);

    // Tìm theo phần cuối của referenceId (vnp_TxnRef nằm cuối "TXN-{timestamp}-{vnp_TxnRef}")
    Optional<WalletTransaction> findByReferenceIdEndingWith(String suffix);

    // Lấy lịch sử giao dịch theo walletId, sắp xếp mới nhất trước
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(String walletId);

    // Lấy lịch sử giao dịch theo walletId với phân trang
    Page<WalletTransaction> findByWalletId(String walletId, Pageable pageable);

    @Query("SELECT tx FROM WalletTransaction tx WHERE tx.type = :type AND tx.status = :status AND tx.createdAt <= :before")
    List<WalletTransaction> findByTypeAndStatusAndCreatedAtBefore(
            @Param("type") WalletTransactionType type,
            @Param("status") WalletTransactionStatus status,
            @Param("before") LocalDateTime before);
}
