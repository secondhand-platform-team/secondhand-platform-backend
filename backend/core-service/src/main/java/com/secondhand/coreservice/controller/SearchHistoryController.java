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

import com.secondhand.coreservice.dto.request.SearchHistoryRequest;
import com.secondhand.coreservice.dto.response.SearchHistoryResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.service.SearchHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping
    public ResponseEntity<SearchHistoryResponse> saveSearchHistory(
            @Valid @RequestBody SearchHistoryRequest request) {

        log.info("Saving search history: {}", request.getSearchQuery());

        SearchHistoryResponse response = searchHistoryService.saveSearchHistory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<SearchHistoryResponse>> getSearchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 50)
            size = 50;
        Pageable pageable = PageRequest.of(page, size);

        Page<SearchHistoryResponse> responses = searchHistoryService.getSearchHistory(pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SearchHistoryResponse>> getRecentSearches() {
        List<SearchHistoryResponse> responses = searchHistoryService.getRecentSearches();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions() {
        List<String> suggestions = searchHistoryService.getSearchSuggestions();
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/trending")
    public ResponseEntity<List<String>> getTrendingSearches() {
        List<String> trending = searchHistoryService.getTrendingSearches();
        return ResponseEntity.ok(trending);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<SearchHistoryResponse>> getSearchHistoryByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 50)
            size = 50;
        Pageable pageable = PageRequest.of(page, size);

        Page<SearchHistoryResponse> responses = searchHistoryService
                .getSearchHistoryByCategory(categoryId, pageable);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteSearchHistory(@PathVariable String id) {
        searchHistoryService.deleteSearchHistory(id);
        return ResponseEntity.ok(MessageResponse.success("Search history deleted"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<MessageResponse> clearSearchHistory() {
        searchHistoryService.clearSearchHistory();
        return ResponseEntity.ok(MessageResponse.success("Search history cleared"));
    }
}
