package com.secondhand.coreservice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.enums.ItemStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {

    /**
     * Pessimistic Lock — SELECT FOR UPDATE
     * Dùng khi reserve item để ngăn Race Condition.
     * 2 buyer gọi cùng lúc → 1 phải chờ transaction kia xong.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Item i WHERE i.itemId = :itemId")
    Optional<Item> findByItemIdForUpdate(@Param("itemId") String itemId);

    /**
     * Tìm các item reservation đã hết hạn (VNPay timeout)
     * Scheduler sẽ chuyển chúng về ACTIVE
     */
    @Query("SELECT i FROM Item i WHERE i.status = 'RESERVED' AND i.reservedUntil IS NOT NULL AND i.reservedUntil < :now")
    List<Item> findExpiredReservations(@Param("now") java.time.LocalDateTime now);

    /**
     * [Tối ưu] Bulk UPDATE: ẩn toàn bộ tin hết hạn bằng 1 câu SQL duy nhất.
     * Không load entity vào RAM — hiệu năng O(1) thay vì O(n).
     * Trả về số rows đã được ẩn.
     */
    @Modifying
    @Query("UPDATE Item i SET i.status = com.secondhand.coreservice.model.enums.ItemStatus.HIDDEN, i.updatedAt = :now "
         + "WHERE i.status = com.secondhand.coreservice.model.enums.ItemStatus.ACTIVE "
         + "AND i.expiredAt IS NOT NULL AND i.expiredAt < :now AND i.deletedAt IS NULL")
    int bulkHideExpiredItems(@Param("now") LocalDateTime now);

    /**
     * Lấy danh sách item đã bị hết hạn (sau khi bulk update) để gửi notification.
     * Dùng Pageable để xử lý theo batch, tránh load toàn bộ vào RAM.
     * Chỉ SELECT các field cần thiết (itemId, userId, title) thay vì toàn bộ entity.
     */
    @Query("SELECT i FROM Item i WHERE i.status = com.secondhand.coreservice.model.enums.ItemStatus.HIDDEN "
         + "AND i.expiredAt IS NOT NULL AND i.expiredAt >= :windowStart AND i.expiredAt < :now "
         + "AND i.deletedAt IS NULL ORDER BY i.expiredAt ASC")
    Page<Item> findRecentlyExpiredItemsForNotification(
            @Param("now") LocalDateTime now,
            @Param("windowStart") LocalDateTime windowStart,
            Pageable pageable);

    /**
     * Query cũ — giữ lại để tương thích với code khác (e.g. unit test).
     * @deprecated Dùng {@link #bulkHideExpiredItems} + {@link #findRecentlyExpiredItemsForNotification} thay thế.
     */
    @Deprecated
    @Query("SELECT i FROM Item i WHERE i.status = com.secondhand.coreservice.model.enums.ItemStatus.ACTIVE "
         + "AND i.expiredAt IS NOT NULL AND i.expiredAt < :now AND i.deletedAt IS NULL")
    List<Item> findExpiredItems(@Param("now") LocalDateTime now);

    @Query("SELECT i FROM Item i WHERE i.category.categoryId = :categoryId AND i.deletedAt IS NULL")
    List<Item> findByCategory_CategoryId(@Param("categoryId") String categoryId);

    /**
     * Tìm item theo category + slug, chỉ lấy các item chưa hết hạn.
     * Lọ expiredAt ngay tại query — tin hết hạn biến mất ngay lập tức không cần đợi scheduler.
     */
    @Query("SELECT i FROM Item i WHERE i.category.slug = :slug AND i.status = :status "
         + "AND i.deletedAt IS NULL "
         + "AND (i.expiredAt IS NULL OR i.expiredAt > CURRENT_TIMESTAMP)")
    List<Item> findByCategory_SlugAndStatus(@Param("slug") String slug, @Param("status") ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.category.slug = :slug AND i.status IN :statuses "
         + "AND i.deletedAt IS NULL "
         + "AND (i.expiredAt IS NULL OR i.expiredAt > CURRENT_TIMESTAMP)")
    List<Item> findByCategory_SlugAndStatusIn(@Param("slug") String slug, @Param("statuses") List<ItemStatus> statuses);

    Optional<Item> findByItemId(String itemId);

    /**
     * ↑ Các query searchItems đã thêm: AND (i.expired_at IS NULL OR i.expired_at > NOW())
     * Buyer không thể thấy tin hết hạn ngay lập tức tại thời điểm đó mà không cần chờ scheduler.
     */
    @Query(value =
           "SELECT i.* FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' AND i.deleted_at IS NULL " +
           "AND (i.expired_at IS NULL OR i.expired_at > NOW()) " +
            "AND (CAST(:keyword AS TEXT) IS NULL OR i.title ILIKE :keyword " +
           "     OR i.description ILIKE :keyword) " +
            "AND (CAST(:categoryId AS TEXT) IS NULL OR i.category_id = :categoryId) " +
           "AND (CAST(:minPrice AS NUMERIC) IS NULL OR i.price >= CAST(:minPrice AS NUMERIC)) " +
           "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR i.price <= CAST(:maxPrice AS NUMERIC)) " +
            "AND (CAST(:condition AS TEXT) IS NULL OR i.condition = :condition) " +
            "AND (CAST(:transactionType AS TEXT) IS NULL OR i.transaction_type = :transactionType) " +
            "AND (CAST(:city AS TEXT) IS NULL OR loc.city ILIKE :city OR i.location ILIKE :city) " +
            "AND (CAST(:district AS TEXT) IS NULL OR loc.district ILIKE :district OR i.location ILIKE :district) " +
            "AND (CAST(:ward AS TEXT) IS NULL OR loc.ward ILIKE :ward OR i.location ILIKE :ward) " +
            "ORDER BY " +
            "CASE WHEN :sort = 'price_asc' THEN i.price END ASC, " +
            "CASE WHEN :sort = 'price_desc' THEN i.price END DESC, " +
            "CASE WHEN :sort = 'oldest' THEN i.created_at END ASC, " +
             "CASE WHEN CAST(:sort AS TEXT) IS NULL OR :sort = 'newest' THEN i.created_at END DESC, " +
            "i.created_at DESC",
           countQuery =
           "SELECT COUNT(*) FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' AND i.deleted_at IS NULL " +
           "AND (i.expired_at IS NULL OR i.expired_at > NOW()) " +
            "AND (CAST(:keyword AS TEXT) IS NULL OR i.title ILIKE :keyword " +
           "     OR i.description ILIKE :keyword) " +
            "AND (CAST(:categoryId AS TEXT) IS NULL OR i.category_id = :categoryId) " +
           "AND (CAST(:minPrice AS NUMERIC) IS NULL OR i.price >= CAST(:minPrice AS NUMERIC)) " +
           "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR i.price <= CAST(:maxPrice AS NUMERIC)) " +
            "AND (CAST(:condition AS TEXT) IS NULL OR i.condition = :condition) " +
            "AND (CAST(:transactionType AS TEXT) IS NULL OR i.transaction_type = :transactionType) " +
               "AND (CAST(:city AS TEXT) IS NULL OR loc.city ILIKE :city OR i.location ILIKE :city) " +
               "AND (CAST(:district AS TEXT) IS NULL OR loc.district ILIKE :district OR i.location ILIKE :district) " +
               "AND (CAST(:ward AS TEXT) IS NULL OR loc.ward ILIKE :ward OR i.location ILIKE :ward)",
           nativeQuery = true)
    Page<Item> searchItems(
            @Param("keyword") String keyword,
            @Param("categoryId") String categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("condition") String condition,
            @Param("transactionType") String transactionType,
            @Param("city") String city,
                @Param("district") String district,
                @Param("ward") String ward,
            @Param("sort") String sort,
            Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND i.deletedAt IS NULL "
         + "AND (i.expiredAt IS NULL OR i.expiredAt > CURRENT_TIMESTAMP) "
         + "ORDER BY i.createdAt DESC")
    List<Item> findTopActiveItems(Pageable pageable);

    /**
     * Search items filtered by a list of categoryIds (parent + all children).
     * Supports full-text keyword, price range, condition, transaction type, location,
     * sorting and pagination.
     */
    @Query(value =
           "SELECT i.* FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' AND i.deleted_at IS NULL " +
           "AND (i.expired_at IS NULL OR i.expired_at > NOW()) " +
           "AND (:categoryIds IS NULL OR i.category_id = ANY(CAST(:categoryIds AS TEXT[]))) " +
           "AND (CAST(:keyword AS TEXT) IS NULL OR i.title ILIKE :keyword " +
           "     OR i.description ILIKE :keyword) " +
           "AND (CAST(:minPrice AS NUMERIC) IS NULL OR i.price >= CAST(:minPrice AS NUMERIC)) " +
           "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR i.price <= CAST(:maxPrice AS NUMERIC)) " +
           "AND (CAST(:condition AS TEXT) IS NULL OR i.condition = :condition) " +
           "AND (CAST(:transactionType AS TEXT) IS NULL OR i.transaction_type = :transactionType) " +
           "AND (CAST(:city AS TEXT) IS NULL OR loc.city ILIKE :city OR i.location ILIKE :city) " +
           "AND (CAST(:district AS TEXT) IS NULL OR loc.district ILIKE :district OR i.location ILIKE :district) " +
           "AND (CAST(:ward AS TEXT) IS NULL OR loc.ward ILIKE :ward OR i.location ILIKE :ward) " +
           "ORDER BY " +
           "CASE WHEN :sort = 'price_asc' THEN i.price END ASC, " +
           "CASE WHEN :sort = 'price_desc' THEN i.price END DESC, " +
           "CASE WHEN :sort = 'oldest' THEN i.created_at END ASC, " +
           "CASE WHEN CAST(:sort AS TEXT) IS NULL OR :sort = 'newest' THEN i.created_at END DESC, " +
           "i.created_at DESC",
           countQuery =
           "SELECT COUNT(*) FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' AND i.deleted_at IS NULL " +
           "AND (i.expired_at IS NULL OR i.expired_at > NOW()) " +
           "AND (:categoryIds IS NULL OR i.category_id = ANY(CAST(:categoryIds AS TEXT[]))) " +
           "AND (CAST(:keyword AS TEXT) IS NULL OR i.title ILIKE :keyword " +
           "     OR i.description ILIKE :keyword) " +
           "AND (CAST(:minPrice AS NUMERIC) IS NULL OR i.price >= CAST(:minPrice AS NUMERIC)) " +
           "AND (CAST(:maxPrice AS NUMERIC) IS NULL OR i.price <= CAST(:maxPrice AS NUMERIC)) " +
           "AND (CAST(:condition AS TEXT) IS NULL OR i.condition = :condition) " +
           "AND (CAST(:transactionType AS TEXT) IS NULL OR i.transaction_type = :transactionType) " +
           "AND (CAST(:city AS TEXT) IS NULL OR loc.city ILIKE :city OR i.location ILIKE :city) " +
           "AND (CAST(:district AS TEXT) IS NULL OR loc.district ILIKE :district OR i.location ILIKE :district) " +
           "AND (CAST(:ward AS TEXT) IS NULL OR loc.ward ILIKE :ward OR i.location ILIKE :ward)",
           nativeQuery = true)
    Page<Item> searchItemsByCategoryIds(
            @Param("categoryIds") String categoryIds,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("condition") String condition,
            @Param("transactionType") String transactionType,
            @Param("city") String city,
            @Param("district") String district,
            @Param("ward") String ward,
            @Param("sort") String sort,
            Pageable pageable);

    // Soft delete queries - exclude deleted items
    @Query("SELECT i FROM Item i WHERE i.userId = :userId AND i.deletedAt IS NULL")
    List<Item> findByUserId(@Param("userId") String userId);

    @Query("SELECT i FROM Item i WHERE i.userId = :userId AND i.deletedAt IS NULL")
    Page<Item> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.deletedAt IS NULL "
         + "AND (i.expiredAt IS NULL OR i.expiredAt > CURRENT_TIMESTAMP)")
    List<Item> findAllByStatus(@Param("status") ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.deletedAt IS NULL "
         + "AND (i.expiredAt IS NULL OR i.expiredAt > CURRENT_TIMESTAMP)")
    Page<Item> findAllByStatus(@Param("status") ItemStatus status, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<Item> findAllNotDeleted();
}

