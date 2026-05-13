package com.secondhand.coreservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secondhand.coreservice.dto.request.ViewHistoryRequest;
import com.secondhand.coreservice.dto.response.ViewHistoryResponse;
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

    @GetMapping("/recent")
    public ResponseEntity<List<ViewHistoryResponse>> getRecentlyViewedItems() {
        List<ViewHistoryResponse> responses = viewHistoryService.getRecentlyViewedItems();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteViewHistory(@PathVariable String id) {
        viewHistoryService.deleteViewHistory(id);
        return ResponseEntity.ok(MessageResponse.success("View history deleted"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<MessageResponse> clearViewHistory() {
        viewHistoryService.clearViewHistory();
        return ResponseEntity.ok(MessageResponse.success("View history cleared"));
    }
}
