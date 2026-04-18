package com.secondhand.coreservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.SearchHistory;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    Page<SearchHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<SearchHistory> findTop10ByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT sh.searchQuery FROM SearchHistory sh " +
            "WHERE sh.userId = :userId " +
            "GROUP BY sh.searchQuery " +
            "ORDER BY MAX(sh.createdAt) DESC")
    List<String> findDistinctSearchQueriesByUserId(@Param("userId") String userId);

    @Query("SELECT sh.searchQuery, COUNT(sh) as count FROM SearchHistory sh " +
            "WHERE sh.createdAt >= :fromDate " +
            "GROUP BY sh.searchQuery " +
            "ORDER BY count DESC " +
            "LIMIT 20")
    List<Object[]> findTrendingSearches(@Param("fromDate") LocalDateTime fromDate);

    long countByUserId(String userId);

    long deleteByCreatedAtBefore(LocalDateTime date);

    Page<SearchHistory> findByUserIdAndCategoryIdOrderByCreatedAtDesc(
            String userId, String categoryId, Pageable pageable);
}
