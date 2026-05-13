package com.secondhand.coreservice.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import com.secondhand.coreservice.dto.request.CategoryRequest;
import com.secondhand.coreservice.dto.request.CategoryAttributeRequest;
import com.secondhand.coreservice.dto.response.CategoryAttributeResponse;
import com.secondhand.coreservice.dto.response.CategoryResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;

public interface CategoryService {
    /**
     * Create a new category with optional parent category reference.
     *
     * @param request category details (name, description, parent ID, attributes)
     * @return created category with generated ID
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Retrieve category by unique identifier.
     *
     * @param categoryId unique category identifier
     * @return category details; throws ResourceNotFoundException if not found
     */
    CategoryResponse getCategoryById(String categoryId);

    /**
     * Retrieve all top-level categories.
     *
     * @return list of all categories in the system
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Retrieve top-level categories (no parent).
     *
     * @return list of root categories for homepage navigation
     */
    List<CategoryResponse> getTopLevelCategories();

    /**
     * Update an existing category.
     *
     * @param categoryId unique category identifier
     * @param request    updated category details
     * @return updated category; throws ResourceNotFoundException if not found
     */
    CategoryResponse updateCategory(String categoryId, CategoryRequest request);

    /**
     * Retrieve category by slug.
     *
     * @param slug category slug
     * @return category details; throws ResourceNotFoundException if not found
     */
    CategoryResponse getCategoryBySlug(String slug);

    /**
     * Delete a category.
     * Category must not have active items or child categories.
     *
     * @param categoryId unique category identifier
     * @return success message; throws ResourceNotFoundException if not found,
     *         BadRequestException if category has dependencies
     */
    MessageResponse deleteCategory(String categoryId);

    /**
     * Retrieve child categories of a specific parent category.
     * Supports category hierarchy navigation.
     *
     * @param parentCategoryId unique identifier of the parent category
     * @return list of child categories; empty list if no children exist
     */
    List<CategoryResponse> getCategoryChildren(String parentCategoryId);

    /**
     * Retrieve child categories by parent slug.
     *
     * @param parentSlug parent category slug
     * @return list of child categories
     */
    List<CategoryResponse> getCategoryChildrenBySlug(String parentSlug);

    /**
     * Retrieve all custom attributes defined for a category.
     * Returns category-specific fields that items must or can have.
     *
     * @param categoryId unique category identifier
     * @return list of category attributes (name, type, required, order)
     */
    List<CategoryAttributeResponse> getCategoryAttributes(String categoryId);

    /**
     * Search items belonging to the given category AND all its descendant categories.
     * Supports keyword search, price range, condition, transaction type, location filtering,
     * sorting and pagination.
     *
     * @param categoryId      root category identifier (items from children included)
     * @param keyword         optional search keyword
     * @param minPrice        optional minimum price
     * @param maxPrice        optional maximum price
     * @param condition       optional item condition filter
     * @param transactionType optional transaction type filter
     * @param city            optional city filter
     * @param district        optional district filter
     * @param ward            optional ward filter
     * @param page            zero-based page index
     * @param size            page size
     * @param sort            sort order (newest, oldest, price_asc, price_desc)
     * @return paginated list of items
     */
    Page<ItemResponse> searchItemsByCategory(
            String categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String condition,
            String transactionType,
            String city,
            String district,
            String ward,
            int page,
            int size,
            String sort);

    CategoryAttributeResponse createCategoryAttribute(String categoryId, CategoryAttributeRequest request);

    CategoryAttributeResponse updateCategoryAttribute(String categoryId, String attributeId, CategoryAttributeRequest request);

    MessageResponse deleteCategoryAttribute(String categoryId, String attributeId);
}
