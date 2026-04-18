package com.secondhand.coreservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.ViewHistory;

@Repository
public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {

        Page<ViewHistory> findByUserIdOrderByViewedAtDesc(String userId, Pageable pageable);

        List<ViewHistory> findTop20ByUserIdOrderByViewedAtDesc(String userId);

        @Query("SELECT DISTINCT vh.item.itemId FROM ViewHistory vh " +
                        "WHERE vh.userId = :userId " +
                        "AND vh.viewedAt >= :fromDate " +
                        "ORDER BY vh.viewedAt DESC")
        List<String> findRecentlyViewedItemIds(
                        @Param("userId") String userId,
                        @Param("fromDate") LocalDateTime fromDate);

        @Query("SELECT vh.item.itemId, COUNT(vh) as viewCount FROM ViewHistory vh " +
                        "WHERE vh.userId = :userId " +
                        "GROUP BY vh.item.itemId " +
                        "ORDER BY viewCount DESC")
        List<Object[]> findMostViewedItemsByUser(@Param("userId") String userId, Pageable pageable);

        @Query("SELECT CASE WHEN COUNT(vh) > 0 THEN true ELSE false END FROM ViewHistory vh WHERE vh.userId = :userId AND vh.item.itemId = :itemId")
        boolean existsByUserIdAndItemId(@Param("userId") String userId, @Param("itemId") String itemId);

        @Query("SELECT vh FROM ViewHistory vh WHERE vh.userId = :userId AND vh.item.itemId = :itemId ORDER BY vh.viewedAt DESC")
        Optional<ViewHistory> findTopByUserIdAndItemIdOrderByViewedAtDesc(@Param("userId") String userId,
                        @Param("itemId") String itemId);

        @Query("SELECT COUNT(vh) FROM ViewHistory vh WHERE vh.userId = :userId AND vh.item.itemId = :itemId")
        long countByUserIdAndItemId(@Param("userId") String userId, @Param("itemId") String itemId);

        long countByUserId(String userId);

        @Query("SELECT COUNT(DISTINCT vh.item.itemId) FROM ViewHistory vh WHERE vh.userId = :userId")
        long countDistinctItemsViewedByUser(@Param("userId") String userId);

        long deleteByViewedAtBefore(LocalDateTime date);

        @Query("SELECT vh.item.itemId, COUNT(vh) as totalViews FROM ViewHistory vh " +
                        "WHERE vh.viewedAt >= :fromDate " +
                        "GROUP BY vh.item.itemId " +
                        "ORDER BY totalViews DESC")
        List<Object[]> findMostViewedItems(
                        @Param("fromDate") LocalDateTime fromDate,
                        Pageable pageable);

        @Query("SELECT vh FROM ViewHistory vh " +
                        "WHERE vh.userId = :userId " +
                        "AND vh.item.category.categoryId = :categoryId " +
                        "ORDER BY vh.viewedAt DESC")
        Page<ViewHistory> findByUserIdAndCategoryId(
                        @Param("userId") String userId,
                        @Param("categoryId") String categoryId,
                        Pageable pageable);
}
