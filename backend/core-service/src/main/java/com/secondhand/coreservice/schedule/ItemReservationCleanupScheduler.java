package com.secondhand.coreservice.schedule;

import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.enums.ItemStatus;
import com.secondhand.coreservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler giải phóng item reservation đã hết hạn.
 * 
 * Khi buyer chọn thanh toán VNPay, item được reserve 10 phút.
 * Nếu buyer không thanh toán trong thời gian này → item trở lại ACTIVE.
 * 
 * Đây là cách Shopee/Tiki xử lý: PAYMENT_PENDING → timeout → ACTIVE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemReservationCleanupScheduler {

    private final ItemRepository itemRepository;

    @Scheduled(fixedRate = 60000) // kiểm tra mỗi 1 phút
    @Transactional
    public void releaseExpiredReservations() {
        List<Item> expiredItems = itemRepository.findExpiredReservations(LocalDateTime.now());

        for (Item item : expiredItems) {
            try {
                log.info("[ReservationCleanup] Releasing expired reservation: itemId={}, reservedBy={}, reservedUntil={}",
                        item.getItemId(), item.getReservedBy(), item.getReservedUntil());

                item.setStatus(ItemStatus.ACTIVE);
                item.setReservedBy(null);
                item.setReservedUntil(null);
                item.setUpdatedAt(LocalDateTime.now());
                itemRepository.save(item);

                log.info("[ReservationCleanup] Item {} released back to ACTIVE", item.getItemId());
            } catch (Exception e) {
                log.error("[ReservationCleanup] Failed to release item {}: {}", 
                        item.getItemId(), e.getMessage(), e);
            }
        }

        if (!expiredItems.isEmpty()) {
            log.info("[ReservationCleanup] Released {} expired reservations", expiredItems.size());
        }
    }
}
