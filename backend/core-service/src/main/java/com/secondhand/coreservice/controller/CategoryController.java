package com.secondhand.coreservice.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.secondhand.coreservice.dto.request.CategoryRequest;
import com.secondhand.coreservice.dto.response.CategoryAttributeResponse;
import com.secondhand.coreservice.dto.response.CategoryResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/top-level")
    public ResponseEntity<List<CategoryResponse>> getTopLevelCategories() {
        List<CategoryResponse> categories = categoryService.getTopLevelCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/slug/{slug}/children")
    public ResponseEntity<List<CategoryResponse>> getCategoryChildrenBySlug(@PathVariable String slug) {
        List<CategoryResponse> children = categoryService.getCategoryChildrenBySlug(slug);
        return ResponseEntity.ok(children);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable String categoryId) {
        CategoryResponse category = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/{categoryId}/children")
    public ResponseEntity<List<CategoryResponse>> getCategoryChildren(@PathVariable String categoryId) {
        List<CategoryResponse> children = categoryService.getCategoryChildren(categoryId);
        return ResponseEntity.ok(children);
    }

    @GetMapping("/{categoryId}/parents")
    public ResponseEntity<List<CategoryResponse>> getCategoryChildrenLegacy(@PathVariable String categoryId) {
        List<CategoryResponse> children = categoryService.getCategoryChildren(categoryId);
        return ResponseEntity.ok(children);
    }

    @GetMapping("/{categoryId}/attributes")
    public ResponseEntity<List<CategoryAttributeResponse>> getCategoryAttributes(@PathVariable String categoryId) {
        List<CategoryAttributeResponse> attributes = categoryService.getCategoryAttributes(categoryId);
        return ResponseEntity.ok(attributes);
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable String categoryId,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable String categoryId) {
        MessageResponse response = categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    /**
     * Search items belonging to the given category and all its sub-categories.
     * Supports keyword, price range, condition, transaction type, location filters,
     * sorting and pagination.
     */
    @GetMapping("/{categoryId}/items")
    public ResponseEntity<Page<ItemResponse>> getItemsByCategory(
            @PathVariable String categoryId,
            @RequestParam(required = false) String q,
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
        Page<ItemResponse> result = categoryService.searchItemsByCategory(
                categoryId, q, minPrice, maxPrice, condition, transactionType,
                city, district, ward, page, size, sort);
        return ResponseEntity.ok(result);
    }
}
