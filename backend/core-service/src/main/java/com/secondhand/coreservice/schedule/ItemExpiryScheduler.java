package com.secondhand.coreservice.schedule;

import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.enums.NotificationType;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduler tự động ẩn các tin đăng đã hết hạn.
 *
 * Chiến lược tối ưu hiệu suất (quan trọng khi có hàng ngàn users):
 *
 * ┌────────────────────────────────────────────────────────────────┐
 * │  BƯỚC 1 — Bulk UPDATE (1 câu SQL duy nhất)                   │
 * │    UPDATE items SET status='HIDDEN'                           │
 * │    WHERE status='ACTIVE' AND expired_at < now                 │
 * │    → N queries → 1 query, không load entity vào RAM           │
 * │                                                               │
 * │  BƯỚC 2 — Gửi notification theo batch (async)               │
 * │    Đọc items đã HIDDEN theo từng trang (200/batch)            │
 * │    Gửi notification bất đồng bộ, không block scheduler        │
 * └────────────────────────────────────────────────────────────────┘
 *
 * So sánh với cách cũ (N items hết hạn):
 *   Cũ:  1 SELECT + N UPDATE + N INSERT = 2N+1 queries
 *   Mới: 1 UPDATE + ceil(N/200) SELECT + N INSERT (async) ≈ N/200 + 2 queries đồng bộ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemExpiryScheduler {

    private final ItemRepository itemRepository;
    private final NotificationService notificationService;

    /** Số item xử lý notification trong 1 batch (tránh OOM với hàng nghìn items) */
    private static final int NOTIFICATION_BATCH_SIZE = 200;

    /** Cửa sổ thời gian để gửi notification — 25 giờ đảm bảo không bỏ sót */
    private static final int SCHEDULER_INTERVAL_HOURS = 25;

    /**
     * Chạy mỗi ngày lúc 2:00 sáng — chỉ để đồng bộ trạng thái DB (status = HIDDEN).
     *
     * ⚠️ QUAN TRỌNG: Việc ẩn tin khỏi buyer KHÔNG phụ thuộc vào scheduler này.
     * Tất cả query public đã lọc (expired_at IS NULL OR expired_at > NOW())
     * nên tin hết hạn biến mất ngay lập tức khi expiredAt quá thời hạn.
     *
     * Scheduler chỉ cập nhật cột `status` để:
     *   - Seller thấy đúng trạng thái "Hết hạn" trong trang quản lý của họ
     *   - Thống kê DB chính xác
     *   - Gửi notification cho seller
     *
     * BƯỚC 1: Bulk UPDATE tất cả tin hết hạn → HIDDEN (1 câu SQL).
     * BƯỚC 2: Gửi notification async theo batch 200 items.
     */
    @Scheduled(cron = "0 0 2 * * *") // 2:00 sáng mỗi ngày
    @Transactional
    public void hideExpiredListings() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("[ExpiryScheduler] Bắt đầu kiểm tra tin đăng hết hạn tại {}", now);

        // ── BƯỚC 1: Bulk UPDATE ──────────────────────────────────────────────
        // Chỉ 1 câu SQL, không load entity vào RAM.
        // Hiệu quả với hàng triệu records vì dùng index idx_items_expired_at.
        int hiddenCount = itemRepository.bulkHideExpiredItems(now);

        if (hiddenCount == 0) {
            log.debug("[ExpiryScheduler] Không có tin nào hết hạn.");
            return;
        }

        log.info("[ExpiryScheduler] Đã ẩn {} tin đăng hết hạn (bulk UPDATE).", hiddenCount);

        // ── BƯỚC 2: Gửi notification async theo batch ────────────────────────
        // Lấy trong cửa sổ [now - 1h, now) để đúng các item vừa được hide trong lần này.
        // Xử lý bất đồng bộ để không block luồng scheduler.
        LocalDateTime windowStart = now.minusHours(SCHEDULER_INTERVAL_HOURS);
        sendExpiryNotificationsAsync(now, windowStart);
    }

    /**
     * Gửi notification hết hạn theo batch — chạy trên thread pool riêng (@Async).
     * Không block scheduler chính, và xử lý từng trang 200 items để tránh OOM.
     *
     * @param now         thời điểm hiện tại (cũng là upper bound của window)
     * @param windowStart lower bound — chỉ gửi cho items expire trong chu kỳ này
     */
    @Async("taskExecutor")
    public void sendExpiryNotificationsAsync(LocalDateTime now, LocalDateTime windowStart) {
        int pageIndex = 0;
        int totalNotified = 0;

        try {
            Page<Item> batch;
            do {
                // Lấy từng trang 200 items — không load toàn bộ vào RAM
                batch = itemRepository.findRecentlyExpiredItemsForNotification(
                        now, windowStart, PageRequest.of(pageIndex, NOTIFICATION_BATCH_SIZE));

                for (Item item : batch.getContent()) {
                    sendSingleExpiryNotification(item);
                    totalNotified++;
                }

                pageIndex++;
            } while (batch.hasNext());

            log.info("[ExpiryScheduler] Đã gửi {} notification hết hạn (async, {} batch).",
                    totalNotified, pageIndex);

        } catch (Exception e) {
            log.error("[ExpiryScheduler] Lỗi khi gửi notification hết hạn: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi notification cho 1 item, bắt lỗi riêng để 1 lỗi không ảnh hưởng batch.
     */
    private void sendSingleExpiryNotification(Item item) {
        try {
            notificationService.createAndSendNotification(
                    item.getUserId(),
                    "Tin đăng \"" + item.getTitle() + "\" của bạn đã hết hạn và bị ẩn. "
                            + "Bấm \"Gia hạn\" để tiếp tục hiển thị.",
                    NotificationType.SYSTEM,
                    item.getItemId());
        } catch (Exception e) {
            log.warn("[ExpiryScheduler] Không gửi được notification cho item {}: {}",
                    item.getItemId(), e.getMessage());
        }
    }
}
