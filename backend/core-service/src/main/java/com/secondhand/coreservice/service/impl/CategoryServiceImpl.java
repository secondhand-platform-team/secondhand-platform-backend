package com.secondhand.coreservice.service.impl;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

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

    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final Pattern LEADING_TRAILING_DASH_PATTERN = Pattern.compile("(^-+)|(-+$)");

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        String slug = buildSlug(request.getSlug(), request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Category with slug '" + slug + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .postingFee(request.getPostingFee() != null ? request.getPostingFee() : 0L)
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
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTopLevelCategories() {
        return categoryRepository.findByParentIsNull().stream()
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

        String slug = buildSlug(request.getSlug(), request.getName());
        if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Category with slug '" + slug + "' already exists");
        }

        category.setName(request.getName());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        if (request.getPostingFee() != null) {
            category.setPostingFee(request.getPostingFee());
        }
        category.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return mapToCategoryResponse(category);
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
        categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + parentCategoryId));

        return categoryRepository.findByParentCategoryId(parentCategoryId).stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryChildrenBySlug(String parentSlug) {
        Category parentCategory = categoryRepository.findBySlug(parentSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + parentSlug));

        return categoryRepository.findByParentCategoryId(parentCategory.getCategoryId()).stream()
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
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .postingFee(category.getPostingFee())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private String buildSlug(String requestedSlug, String name) {
        String raw = requestedSlug != null && !requestedSlug.isBlank() ? requestedSlug : name;

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        String slug = NON_ALNUM_PATTERN.matcher(normalized).replaceAll("-");
        slug = LEADING_TRAILING_DASH_PATTERN.matcher(slug).replaceAll("");

        if (slug.isBlank()) {
            throw new BadRequestException("Category slug is invalid");
        }

        return slug;
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
