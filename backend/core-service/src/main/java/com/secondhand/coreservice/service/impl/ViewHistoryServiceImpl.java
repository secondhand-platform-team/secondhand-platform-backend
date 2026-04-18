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
import com.secondhand.coreservice.dto.request.ViewHistoryRequest;
import com.secondhand.coreservice.dto.response.ViewHistoryResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.model.ViewHistory;
import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.repository.ViewHistoryRepository;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.ViewHistoryService;
import com.secondhand.coreservice.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ViewHistoryServiceImpl implements ViewHistoryService {

    private final ViewHistoryRepository viewHistoryRepository;
    private final ItemRepository itemRepository;
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
    public ViewHistoryResponse saveViewHistory(ViewHistoryRequest request) {
        String userId = getCurrentUserId();

        log.info("Saving view history for user: {} on item: {}", userId, request.getItemId());

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));

        ViewHistory history = ViewHistory.builder()
                .userId(userId)
                .item(item)
                .sessionId(request.getSessionId())
                .build();

        ViewHistory saved = viewHistoryRepository.save(history);
        log.debug("View history saved with ID: {}", saved.getId());

        long viewCount = viewHistoryRepository.countByUserIdAndItemId(userId, request.getItemId());

        return mapToResponse(saved, null, viewCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewHistoryResponse> getViewHistory(Pageable pageable) {
        String userId = getCurrentUserId();

        log.debug("Getting view history for user: {}", userId);

        Page<ViewHistory> page = viewHistoryRepository
                .findByUserIdOrderByViewedAtDesc(userId, pageable);

        return page.map(vh -> mapToResponse(vh, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewHistoryResponse> getRecentlyViewedItems() {
        String userId = getCurrentUserId();

        log.debug("Getting recently viewed items for user: {}", userId);

        List<ViewHistory> histories = viewHistoryRepository
                .findTop20ByUserIdOrderByViewedAtDesc(userId);

        return histories.stream()
                .map(vh -> mapToResponse(vh, null, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewHistoryResponse> getMostViewedItems(Pageable pageable) {
        String userId = getCurrentUserId();

        log.debug("Getting most viewed items for user: {}", userId);

        List<Object[]> results = viewHistoryRepository
                .findMostViewedItemsByUser(userId, pageable);

        return results.stream()
                .map(row -> {
                    String itemId = (String) row[0];
                    Long viewCount = (Long) row[1];

                    return ViewHistoryResponse.builder()
                            .itemId(itemId)
                            .viewCount(viewCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewHistoryResponse> getViewHistoryByCategory(String categoryId, Pageable pageable) {
        String userId = getCurrentUserId();

        log.debug("Getting view history by category: {} for user: {}", categoryId, userId);

        Page<ViewHistory> page = viewHistoryRepository
                .findByUserIdAndCategoryId(userId, categoryId, pageable);

        return page.map(vh -> mapToResponse(vh, null, null));
    }

    @Override
    public void deleteViewHistory(Long id) {
        log.info("Deleting view history record with ID: {}", id);

        ViewHistory history = viewHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("View history not found"));

        String userId = getCurrentUserId();
        if (!history.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to delete this view history");
        }

        viewHistoryRepository.delete(history);
    }

    @Override
    public void clearViewHistory() {
        String userId = getCurrentUserId();

        log.info("Clearing all view history for user: {}", userId);

        long count = viewHistoryRepository.countByUserId(userId);

        List<ViewHistory> allHistory = viewHistoryRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        viewHistoryRepository.deleteAll(allHistory);

        log.info("Cleared {} view history records for user: {}", count, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isItemViewed(String itemId) {
        String userId = getCurrentUserId();

        return viewHistoryRepository.existsByUserIdAndItemId(userId, itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getItemViewCount(String itemId) {
        String userId = getCurrentUserId();

        return viewHistoryRepository.countByUserIdAndItemId(userId, itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalDistinctItemsViewed() {
        String userId = getCurrentUserId();

        return viewHistoryRepository.countDistinctItemsViewedByUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getRecommendations(int limit) {
        String userId = getCurrentUserId();

        log.debug("Getting recommendations for user: {} (limit: {})", userId, limit);

        // Lấy các items được xem nhiều nhất trong 30 ngày gần đây
        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> mostViewed = viewHistoryRepository
                .findMostViewedItems(fromDate, pageable);

        return mostViewed.stream()
                .map(row -> {
                    String itemId = (String) row[0];
                    return itemRepository.findById(itemId)
                            .orElse(null);
                })
                .filter(item -> item != null)
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRecentlyViewedItemIds() {
        String userId = getCurrentUserId();

        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);

        return viewHistoryRepository.findRecentlyViewedItemIds(userId, fromDate);
    }

    private ViewHistoryResponse mapToResponse(ViewHistory history, ItemResponse itemResponse, Long viewCount) {
        return ViewHistoryResponse.builder()
                .id(history.getId())
                .itemId(history.getItem() != null ? history.getItem().getItemId() : null)
                .viewedAt(history.getViewedAt())
                .sessionId(history.getSessionId())
                .item(itemResponse)
                .viewCount(viewCount)
                .build();
    }

    private ItemResponse mapItemToResponse(Item item) {
        return objectMapper.convertValue(item, ItemResponse.class);
    }

    public void cleanupOldViewHistory() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        long deletedCount = viewHistoryRepository.deleteByViewedAtBefore(cutoffDate);

        log.info("Cleaned up {} old view history records", deletedCount);
    }
}
