package com.secondhand.coreservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.secondhand.coreservice.dto.request.CategoryRequest;
import com.secondhand.coreservice.dto.response.CategoryAttributeResponse;
import com.secondhand.coreservice.dto.response.CategoryResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.exception.ResourceNotFoundException;
import com.secondhand.coreservice.model.Category;
import com.secondhand.coreservice.model.CategoryAttribute;
import com.secondhand.coreservice.repository.CategoryAttributeRepository;
import com.secondhand.coreservice.repository.CategoryRepository;
import com.secondhand.coreservice.service.CategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        return mapToCategoryResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse updateCategory(String categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (!category.getName().equals(request.getName()) && 
            categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(updatedCategory);
    }

    @Override
    public MessageResponse deleteCategory(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
        return new MessageResponse("Category deleted successfully", true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryChildren(String parentCategoryId) {
        Category parentCategory = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + parentCategoryId));
        
        return parentCategory.getChildren().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryAttributeResponse> getCategoryAttributes(String categoryId) {
        // Kiểm tra category có tồn tại
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        
        List<CategoryAttribute> attributes = categoryAttributeRepository.findByCategoryCategoryId(categoryId);
        return attributes.stream()
                .map(this::mapToCategoryAttributeResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private CategoryAttributeResponse mapToCategoryAttributeResponse(CategoryAttribute attribute) {
        return CategoryAttributeResponse.builder()
                .attributeId(attribute.getAttributeId())
                .code(attribute.getCode())
                .name(attribute.getName())
                .description(attribute.getDescription())
                .dataType(attribute.getDataType().toString())
                .unit(attribute.getUnit())
                .required(attribute.getRequired())
                .filterable(attribute.getFilterable())
                .searchable(attribute.getSearchable())
                .minValueNumber(attribute.getMinValueNumber())
                .maxValueNumber(attribute.getMaxValueNumber())
                .optionsJson(attribute.getOptionsJson())
                .sortOrder(attribute.getSortOrder())
                .createdAt(attribute.getCreatedAt())
                .updatedAt(attribute.getUpdatedAt())
                .build();
    }
}
