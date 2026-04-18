package com.secondhand.coreservice.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.secondhand.coreservice.dto.request.ViewHistoryRequest;
import com.secondhand.coreservice.dto.response.ViewHistoryResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.service.ViewHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/view-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class ViewHistoryController {

    private final ViewHistoryService viewHistoryService;

    @PostMapping
    public ResponseEntity<ViewHistoryResponse> saveViewHistory(
            @Valid @RequestBody ViewHistoryRequest request) {

        log.info("Saving view history for item: {}", request.getItemId());

        ViewHistoryResponse response = viewHistoryService.saveViewHistory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ViewHistoryResponse>> getViewHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 50)
            size = 50;
        Pageable pageable = PageRequest.of(page, size);

        Page<ViewHistoryResponse> responses = viewHistoryService.getViewHistory(pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ViewHistoryResponse>> getRecentlyViewedItems() {
        List<ViewHistoryResponse> responses = viewHistoryService.getRecentlyViewedItems();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/most-viewed")
    public ResponseEntity<List<ViewHistoryResponse>> getMostViewedItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 50)
            size = 50;
        Pageable pageable = PageRequest.of(page, size);

        List<ViewHistoryResponse> responses = viewHistoryService.getMostViewedItems(pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ViewHistoryResponse>> getViewHistoryByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 50)
            size = 50;
        Pageable pageable = PageRequest.of(page, size);

        Page<ViewHistoryResponse> responses = viewHistoryService
                .getViewHistoryByCategory(categoryId, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<ItemResponse>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit) {

        if (limit > 50)
            limit = 50;

        List<ItemResponse> recommendations = viewHistoryService.getRecommendations(limit);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/stats")
    public ResponseEntity<ViewStatsResponse> getViewStats() {
        long totalDistinct = viewHistoryService.getTotalDistinctItemsViewed();
        List<String> recentIds = viewHistoryService.getRecentlyViewedItemIds();

        ViewStatsResponse stats = ViewStatsResponse.builder()
                .totalDistinctItemsViewed(totalDistinct)
                .recentlyViewedCount(recentIds.size())
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/check/{itemId}")
    public ResponseEntity<ItemViewStatusResponse> checkItemViewed(@PathVariable String itemId) {
        boolean viewed = viewHistoryService.isItemViewed(itemId);
        long viewCount = viewHistoryService.getItemViewCount(itemId);

        ItemViewStatusResponse response = ItemViewStatusResponse.builder()
                .itemId(itemId)
                .viewed(viewed)
                .viewCount(viewCount)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteViewHistory(@PathVariable Long id) {
        viewHistoryService.deleteViewHistory(id);
        return ResponseEntity.ok(MessageResponse.success("View history deleted"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<MessageResponse> clearViewHistory() {
        viewHistoryService.clearViewHistory();
        return ResponseEntity.ok(MessageResponse.success("View history cleared"));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ViewStatsResponse {
        private long totalDistinctItemsViewed;
        private int recentlyViewedCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ItemViewStatusResponse {
        private String itemId;
        private boolean viewed;
        private long viewCount;
    }
}
