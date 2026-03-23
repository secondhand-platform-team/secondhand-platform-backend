package com.secondhand.coreservice.service;

import java.util.List;

import com.secondhand.coreservice.dto.request.CategoryRequest;
import com.secondhand.coreservice.dto.response.CategoryAttributeResponse;
import com.secondhand.coreservice.dto.response.CategoryResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse getCategoryById(String categoryId);
    List<CategoryResponse> getAllCategories();
    CategoryResponse updateCategory(String categoryId, CategoryRequest request);
    MessageResponse deleteCategory(String categoryId);
    List<CategoryResponse> getCategoryChildren(String parentCategoryId);
    List<CategoryAttributeResponse> getCategoryAttributes(String categoryId);
}
