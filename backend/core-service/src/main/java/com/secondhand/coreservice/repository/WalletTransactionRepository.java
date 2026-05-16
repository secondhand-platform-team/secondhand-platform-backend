package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
