package com.secondhand.coreservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.secondhand.coreservice.dto.request.ViewHistoryRequest;
import com.secondhand.coreservice.dto.response.ViewHistoryResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;

public interface ViewHistoryService {

    ViewHistoryResponse saveViewHistory(ViewHistoryRequest request);

    Page<ViewHistoryResponse> getViewHistory(Pageable pageable);

    List<ViewHistoryResponse> getRecentlyViewedItems();

    List<ViewHistoryResponse> getMostViewedItems(Pageable pageable);

    Page<ViewHistoryResponse> getViewHistoryByCategory(String categoryId, Pageable pageable);

    void deleteViewHistory(String id);

    void clearViewHistory();

    boolean isItemViewed(String itemId);

    long getItemViewCount(String itemId);

    long getTotalDistinctItemsViewed();

    List<ItemResponse> getRecommendations(int limit);

    List<String> getRecentlyViewedItemIds();
}
