package com.secondhand.coreservice.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.enums.ItemStatus;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {
    List<Item> findByCategory_CategoryId(String categoryId);
    List<Item> findByCategory_SlugAndStatus(String slug, ItemStatus status);
    List<Item> findByCategory_SlugAndStatusIn(String slug, List<ItemStatus> statuses);

    List<Item> findByUserId(String userId);

    Optional<Item> findByItemId(String itemId);

    List<Item> findAllByStatus(ItemStatus status);

    Page<Item> findByUserId(String userId, Pageable pageable);

    Page<Item> findAllByStatus(ItemStatus status, Pageable pageable);

    @Query(value =
           "SELECT i.* FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' " +
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
           "WHERE i.status = 'ACTIVE' " +
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

    @Query("SELECT i FROM Item i WHERE i.status = 'ACTIVE' ORDER BY i.createdAt DESC")
    List<Item> findTopActiveItems(Pageable pageable);

    /**
     * Search items filtered by a list of categoryIds (parent + all children).
     * Supports full-text keyword, price range, condition, transaction type, location,
     * sorting and pagination.
     */
    @Query(value =
           "SELECT i.* FROM items i LEFT JOIN locations loc ON loc.item_id = i.item_id " +
           "WHERE i.status = 'ACTIVE' " +
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
           "WHERE i.status = 'ACTIVE' " +
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
}

