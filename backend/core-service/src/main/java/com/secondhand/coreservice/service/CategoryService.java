package com.secondhand.coreservice.service;

import java.util.List;

import com.secondhand.coreservice.dto.request.CategoryRequest;
import com.secondhand.coreservice.dto.response.CategoryAttributeResponse;
import com.secondhand.coreservice.dto.response.CategoryResponse;
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
     * Update an existing category.
     *
     * @param categoryId unique category identifier
     * @param request    updated category details
     * @return updated category; throws ResourceNotFoundException if not found
     */
    CategoryResponse updateCategory(String categoryId, CategoryRequest request);

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
     * Retrieve all custom attributes defined for a category.
     * Returns category-specific fields that items must or can have.
     *
     * @param categoryId unique category identifier
     * @return list of category attributes (name, type, required, order)
     */
    List<CategoryAttributeResponse> getCategoryAttributes(String categoryId);
}
