package com.secondhand.coreservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondhand.coreservice.dto.request.SearchHistoryRequest;
import com.secondhand.coreservice.dto.response.SearchHistoryResponse;
import com.secondhand.coreservice.model.SearchHistory;
import com.secondhand.coreservice.repository.SearchHistoryRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.SearchHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final ObjectMapper objectMapper;

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticatedUser) {
            JwtAuthenticatedUser user = (JwtAuthenticatedUser) authentication.getPrincipal();
            return user.userId();
        }
        throw new RuntimeException("Unauthorized");
    }

    @Override
    public SearchHistoryResponse saveSearchHistory(SearchHistoryRequest request) {
        String userId = getCurrentUserId();

        log.info("Saving search history for user: {} with query: {}", userId, request.getSearchQuery());

        String query = request.getSearchQuery().trim();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        SearchHistory history = SearchHistory.builder()
                .userId(userId)
                .searchQuery(query)
                .categoryId(request.getCategoryId())
                .resultCount(request.getResultCount())
                .build();

        SearchHistory saved = searchHistoryRepository.save(history);
        log.debug("Search history saved with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchHistoryResponse> getSearchHistory(Pageable pageable) {
        String userId = getCurrentUserId();

        log.debug("Getting search history for user: {}", userId);

        Page<SearchHistory> page = searchHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getRecentSearches() {
        String userId = getCurrentUserId();

        log.debug("Getting recent searches for user: {}", userId);

        List<SearchHistory> searches = searchHistoryRepository
                .findTop10ByUserIdOrderByCreatedAtDesc(userId);

        return searches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions() {
        String userId = getCurrentUserId();

        log.debug("Getting search suggestions for user: {}", userId);

        List<String> suggestions = searchHistoryRepository
                .findDistinctSearchQueriesByUserId(userId);

        return suggestions.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTrendingSearches() {
        log.debug("Getting trending searches");

        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        List<Object[]> results = searchHistoryRepository.findTrendingSearches(fromDate);

        return results.stream()
                .map(row -> (String) row[0])
                .limit(20)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSearchHistory(Long id) {
        log.info("Deleting search history record with ID: {}", id);

        SearchHistory history = searchHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Search history not found"));

        String userId = getCurrentUserId();
        if (!history.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to delete this search history");
        }

        searchHistoryRepository.delete(history);
    }

    @Override
    public void clearSearchHistory() {
        String userId = getCurrentUserId();

        log.info("Clearing all search history for user: {}", userId);

        long count = searchHistoryRepository.countByUserId(userId);

        List<SearchHistory> allHistory = searchHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        searchHistoryRepository.deleteAll(allHistory);

        log.info("Cleared {} search history records for user: {}", count, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SearchHistoryResponse> getSearchHistoryByCategory(String categoryId, Pageable pageable) {
        String userId = getCurrentUserId();

        log.debug("Getting search history by category: {} for user: {}", categoryId, userId);

        Page<SearchHistory> page = searchHistoryRepository
                .findByUserIdAndCategoryIdOrderByCreatedAtDesc(userId, categoryId, pageable);

        return page.map(this::mapToResponse);
    }

    private SearchHistoryResponse mapToResponse(SearchHistory history) {
        return SearchHistoryResponse.builder()
                .id(history.getId())
                .searchQuery(history.getSearchQuery())
                .categoryId(history.getCategoryId())
                .resultCount(history.getResultCount())
                .createdAt(history.getCreatedAt())
                .build();
    }

    @Transactional
    public void cleanupOldSearchHistory() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        long deletedCount = searchHistoryRepository.deleteByCreatedAtBefore(cutoffDate);

        log.info("Cleaned up {} old search history records", deletedCount);
    }
}
