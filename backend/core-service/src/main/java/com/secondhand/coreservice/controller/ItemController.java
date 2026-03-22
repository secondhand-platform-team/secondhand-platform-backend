package com.secondhand.coreservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.secondhand.coreservice.dto.request.ItemRequest;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.service.ItemService;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ItemController {

    private final ItemService itemService;
    private final Validator validator;

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ItemResponse> createItem(
            @RequestPart("item") String itemJson,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ItemRequest request = mapper.readValue(itemJson, ItemRequest.class);

            // Validate request
            var violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String errorMsg = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Validation error");
                throw new com.secondhand.coreservice.exception.BadRequestException(errorMsg);
            }

            ItemResponse response = itemService.createItem(request, images);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (com.secondhand.coreservice.exception.BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new com.secondhand.coreservice.exception.BadRequestException(
                    "Invalid item JSON: " + e.getMessage());
        }
    }

    @PostMapping(path = "/json", consumes = { "application/json" })
    public ResponseEntity<ItemResponse> createItemJson(@Valid @RequestBody ItemRequest request) {
        ItemResponse response = itemService.createItem(request, null);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<ItemResponse> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable String itemId) {
        ItemResponse item = itemService.getItemById(itemId);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ItemResponse>> getItemsByCategory(@PathVariable String categoryId) {
        List<ItemResponse> items = itemService.getItemsByCategory(categoryId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ItemResponse>> getItemsByUser(@PathVariable String userId) {
        List<ItemResponse> items = itemService.getItemsByUser(userId);
        return ResponseEntity.ok(items);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody ItemRequest request) {
        ItemResponse response = itemService.updateItem(itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<MessageResponse> deleteItem(@PathVariable String itemId) {
        MessageResponse response = itemService.deleteItem(itemId);
        return ResponseEntity.ok(response);
    }
}
