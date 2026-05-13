package com.secondhand.coreservice.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.secondhand.coreservice.dto.request.ItemRequest;
import com.secondhand.coreservice.dto.request.ItemStatusUpdateRequest;
import com.secondhand.coreservice.dto.request.VNPayCallbackRequest;
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

    @PostMapping
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

    @GetMapping("/search")
    public ResponseEntity<Page<ItemResponse>> searchItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String ward,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        if (size > 50) size = 50;
        Page<ItemResponse> items = itemService.searchItems(
                q, categoryId, minPrice, maxPrice, condition, transactionType, city, district, ward, page, size, sort);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ItemResponse>> getFeaturedItems(
            @RequestParam(defaultValue = "4") int limit) {
        if (limit > 20) limit = 20;
        List<ItemResponse> items = itemService.getFeaturedItems(limit);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ItemResponse>> getMyItems() {
        List<ItemResponse> items = itemService.getMyItems();
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

    @GetMapping("/category/slug/{slug}")
    public ResponseEntity<List<ItemResponse>> getItemsByCategorySlug(@PathVariable String slug) {
        List<ItemResponse> items = itemService.getItemsByCategorySlug(slug);
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

    @PatchMapping("/{itemId}/status")
    public ResponseEntity<ItemResponse> updateItemStatus(
            @PathVariable String itemId,
            @Valid @RequestBody ItemStatusUpdateRequest request) {
        ItemResponse response = itemService.updateItemStatus(itemId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<MessageResponse> deleteItem(@PathVariable String itemId) {
        System.out.println("đã vô tới đây");
        MessageResponse response = itemService.deleteItem(itemId);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/test")
    public void test() {
        System.out.println("đã vô tới đây");
    }
    @GetMapping("/test")
    public void test2() {
        System.out.println("đã vô tới đây");
    }
    @PostMapping("/{itemId}/favorite")
    public ResponseEntity<MessageResponse> addFavorite(@PathVariable String itemId) {
        MessageResponse response = itemService.addFavoriteItem(itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}/favorite")
    public ResponseEntity<MessageResponse> removeFavorite(@PathVariable String itemId) {
        MessageResponse response = itemService.removeFavoriteItem(itemId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/favorites/me")
    public ResponseEntity<List<ItemResponse>> getMyFavoriteItems() {
        List<ItemResponse> items = itemService.getMyFavoriteItems();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/payment-callback")
    public ResponseEntity<?> handleVNPayCallback(
            @RequestParam(required = false) String vnp_Amount,
            @RequestParam(required = false) String vnp_BankCode,
            @RequestParam(required = false) String vnp_BankTranNo,
            @RequestParam(required = false) String vnp_CardType,
            @RequestParam(required = false) String vnp_OrderInfo,
            @RequestParam(required = false) String vnp_PayDate,
            @RequestParam(required = false) String vnp_ResponseCode,
            @RequestParam(required = false) String vnp_TmnCode,
            @RequestParam(required = false) String vnp_TransactionNo,
            @RequestParam(required = false) String vnp_TransactionStatus,
            @RequestParam(required = false) String vnp_TxnRef,
            @RequestParam(required = false) String vnp_SecureHash) {
        try {
            VNPayCallbackRequest request = VNPayCallbackRequest.builder()
                    .vnp_Amount(vnp_Amount)
                    .vnp_BankCode(vnp_BankCode)
                    .vnp_BankTranNo(vnp_BankTranNo)
                    .vnp_CardType(vnp_CardType)
                    .vnp_OrderInfo(vnp_OrderInfo)
                    .vnp_PayDate(vnp_PayDate)
                    .vnp_ResponseCode(vnp_ResponseCode)
                    .vnp_TmnCode(vnp_TmnCode)
                    .vnp_TransactionNo(vnp_TransactionNo)
                    .vnp_TransactionStatus(vnp_TransactionStatus)
                    .vnp_TxnRef(vnp_TxnRef)
                    .vnp_SecureHash(vnp_SecureHash)
                    .build();

            itemService.handleVNPayCallback(request);

            // Redirect to frontend success page
            String successUrl = "http://localhost:3000/payment-success?status=success&transactionId="
                    + vnp_TransactionNo;
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(successUrl))
                    .build();
        } catch (Exception e) {
            // Redirect to frontend error page
            String errorUrl = "http://localhost:3000/payment-failed?status=error&message=" +
                    java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
    }
}

