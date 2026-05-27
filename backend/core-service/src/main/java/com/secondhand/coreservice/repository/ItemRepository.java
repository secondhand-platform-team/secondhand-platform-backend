package com.secondhand.coreservice.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT i FROM Item i WHERE i.category.categoryId = :categoryId AND i.deletedAt IS NULL")
    List<Item> findByCategory_CategoryId(@Param("categoryId") String categoryId);

    @Query("SELECT i FROM Item i WHERE i.category.slug = :slug AND i.status = :status AND i.deletedAt IS NULL")
    List<Item> findByCategory_SlugAndStatus(@Param("slug") String slug, @Param("status") ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.category.slug = :slug AND i.status IN :statuses AND i.deletedAt IS NULL")
    List<Item> findByCategory_SlugAndStatusIn(@Param("slug") String slug, @Param("statuses") List<ItemStatus> statuses);

    Optional<Item> findByItemId(String itemId);

    @Query(value =
           "SELECT i.* FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' AND i.deleted_at IS NULL " +
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

    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' AND i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<Item> findTopActiveItems(Pageable pageable);

    /**
     * Search items filtered by a list of categoryIds (parent + all children).
     * Supports full-text keyword, price range, condition, transaction type, location,
     * sorting and pagination.
     */
    @Query(value =
           "SELECT i.* FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' AND i.deleted_at IS NULL " +
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

    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.deletedAt IS NULL")
    List<Item> findAllByStatus(@Param("status") ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.status = :status AND i.deletedAt IS NULL")
    Page<Item> findAllByStatus(@Param("status") ItemStatus status, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    List<Item> findAllNotDeleted();
}

