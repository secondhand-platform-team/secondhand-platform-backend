package com.secondhand.coreservice.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.secondhand.coreservice.dto.request.SearchHistoryRequest;
import com.secondhand.coreservice.dto.response.SearchHistoryResponse;

public interface SearchHistoryService {

    SearchHistoryResponse saveSearchHistory(SearchHistoryRequest request);

    Page<SearchHistoryResponse> getSearchHistory(Pageable pageable);

    List<SearchHistoryResponse> getRecentSearches();

    List<String> getSearchSuggestions();

    List<String> getTrendingSearches();

    void deleteSearchHistory(String id);

    void clearSearchHistory();

    Page<SearchHistoryResponse> getSearchHistoryByCategory(String categoryId, Pageable pageable);
}
