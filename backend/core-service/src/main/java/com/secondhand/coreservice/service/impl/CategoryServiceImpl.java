package com.secondhand.coreservice.service.impl;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.secondhand.coreservice.dto.request.CategoryRequest;
import com.secondhand.coreservice.dto.response.CategoryAttributeResponse;
import com.secondhand.coreservice.dto.response.CategoryResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.exception.ResourceNotFoundException;
import com.secondhand.coreservice.model.Category;
import com.secondhand.coreservice.model.CategoryAttribute;
import com.secondhand.coreservice.repository.CategoryAttributeRepository;
import com.secondhand.coreservice.repository.CategoryRepository;
import com.secondhand.coreservice.repository.ItemRepository;
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
    private final ItemRepository itemRepository;

    @Override
    @CacheEvict(cacheNames = {"categoriesAll", "categoriesTopLevel", "categoriesChildren", "categoriesChildrenBySlug"}, allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        String slug = buildSlug(request.getSlug(), request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Category with slug '" + slug + "' already exists");
        }

        Category parent = null;
        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .postingFee(request.getPostingFee() != null ? request.getPostingFee() : 0L)
                .parent(parent)
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
    @Cacheable(cacheNames = "categoriesAll")
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoriesTopLevel")
    public List<CategoryResponse> getTopLevelCategories() {
        return categoryRepository.findByParentIsNull().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {"categoriesAll", "categoriesTopLevel", "categoriesChildren", "categoriesChildrenBySlug"}, allEntries = true)
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
        if (request.getParentId() != null && !request.getParentId().isBlank()) {
            if (request.getParentId().equals(categoryId)) {
                throw new BadRequestException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
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
    @CacheEvict(cacheNames = {"categoriesAll", "categoriesTopLevel", "categoriesChildren", "categoriesChildrenBySlug"}, allEntries = true)
    public MessageResponse deleteCategory(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
        return new MessageResponse("Category deleted successfully", true);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoriesChildren", key = "#parentCategoryId")
    public List<CategoryResponse> getCategoryChildren(String parentCategoryId) {
        categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + parentCategoryId));

        return categoryRepository.findByParentCategoryId(parentCategoryId).stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoriesChildrenBySlug", key = "#parentSlug")
    public List<CategoryResponse> getCategoryChildrenBySlug(String parentSlug) {
        Category parentCategory = categoryRepository.findBySlug(parentSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + parentSlug));

        return categoryRepository.findByParentCategoryId(parentCategory.getCategoryId()).stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categoryAttributes", key = "#categoryId")
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

    /**
     * Collect all descendant category IDs (BFS) starting from the given category.
     * The root itself is included.
     */
    private List<String> getAllDescendantCategoryIds(String rootCategoryId) {
        List<String> result = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(rootCategoryId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            List<Category> children = categoryRepository.findByParentCategoryId(current);
            children.forEach(c -> queue.add(c.getCategoryId()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "itemsSearchByCategory", key = "T(String).format('%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s', #categoryId, #keyword, #minPrice, #maxPrice, #condition, #transactionType, #city, #district, #ward, #page, #size, #sort)")
    public Page<ItemResponse> searchItemsByCategory(
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
            String sort) {

        // Validate category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        // Collect the root + all descendant category IDs
        List<String> categoryIds = getAllDescendantCategoryIds(categoryId);

        // Build PostgreSQL array literal: {id1,id2,...}
        String pgArray = "{" + String.join(",", categoryIds) + "}";

        String kw = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
        String cond = (condition != null && !condition.isBlank()) ? condition : null;
        String txType = (transactionType != null && !transactionType.isBlank()) ? transactionType : null;
        String cityVal = (city != null && !city.isBlank()) ? "%" + city.trim() + "%" : null;
        String districtVal = (district != null && !district.isBlank()) ? "%" + district.trim() + "%" : null;
        String wardVal = (ward != null && !ward.isBlank()) ? "%" + ward.trim() + "%" : null;
        String sortVal = (sort != null && !sort.isBlank()) ? sort : "newest";

        Pageable pageable = PageRequest.of(page, size);

        return itemRepository.searchItemsByCategoryIds(
                pgArray, kw, minPrice, maxPrice, cond, txType, cityVal, districtVal, wardVal, sortVal, pageable)
                .map(this::mapToItemResponse);
    }

    private ItemResponse mapToItemResponse(com.secondhand.coreservice.model.Item item) {
        com.secondhand.coreservice.dto.response.LocationResponse locationResponse = null;
        if (item.getItemLocation() != null) {
            locationResponse = com.secondhand.coreservice.dto.response.LocationResponse.builder()
                    .address(item.getItemLocation().getAddress())
                    .ward(item.getItemLocation().getWard())
                    .district(item.getItemLocation().getDistrict())
                    .city(item.getItemLocation().getCity())
                    .build();
        }

        List<com.secondhand.coreservice.dto.response.ItemImageResponse> imageResponses = new ArrayList<>();
        if (item.getItemImageList() != null && !item.getItemImageList().isEmpty()) {
            imageResponses = item.getItemImageList().stream()
                    .map(img -> com.secondhand.coreservice.dto.response.ItemImageResponse.builder()
                            .imageUrl(img.getUrl())
                            .isPrimary(img.getIsPrimary())
                            .build())
                    .collect(Collectors.toList());
        }

        return ItemResponse.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .transactionType(item.getTransactionType() != null ? item.getTransactionType().toString() : null)
                .condition(item.getCondition() != null ? item.getCondition().toString() : null)
                .status(item.getStatus() != null ? item.getStatus().toString() : null)
                .userId(item.getUserId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .categoryId(item.getCategory() != null ? item.getCategory().getCategoryId() : null)
                .location(locationResponse)
                .itemImageList(imageResponses)
                .build();
    }

    @Override
    @CacheEvict(cacheNames = "categoryAttributes", key = "#categoryId")
    public CategoryAttributeResponse createCategoryAttribute(String categoryId, com.secondhand.coreservice.dto.request.CategoryAttributeRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        if (categoryAttributeRepository.existsByCategory_CategoryIdAndCode(categoryId, request.getCode())) {
            throw new BadRequestException("Attribute with code '" + request.getCode() + "' already exists in this category");
        }

        com.secondhand.coreservice.model.enums.AttributeDataType dataTypeEnum;
        try {
            dataTypeEnum = com.secondhand.coreservice.model.enums.AttributeDataType.valueOf(request.getDataType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid data type: " + request.getDataType());
        }

        CategoryAttribute attribute = CategoryAttribute.builder()
                .category(category)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .dataType(dataTypeEnum)
                .unit(request.getUnit())
                .required(request.getRequired())
                .filterable(request.getFilterable())
                .searchable(request.getSearchable())
                .minValueNumber(request.getMinValueNumber())
                .maxValueNumber(request.getMaxValueNumber())
                .optionsJson(request.getOptionsJson())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CategoryAttribute savedAttribute = categoryAttributeRepository.save(attribute);
        return mapToCategoryAttributeResponse(savedAttribute);
    }

    @Override
    @CacheEvict(cacheNames = "categoryAttributes", key = "#categoryId")
    public CategoryAttributeResponse updateCategoryAttribute(String categoryId, String attributeId, com.secondhand.coreservice.dto.request.CategoryAttributeRequest request) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + attributeId));

        if (!attribute.getCategory().getCategoryId().equals(categoryId)) {
            throw new BadRequestException("Attribute does not belong to the specified category");
        }

        if (!attribute.getCode().equals(request.getCode()) &&
            categoryAttributeRepository.existsByCategory_CategoryIdAndCode(categoryId, request.getCode())) {
            throw new BadRequestException("Attribute with code '" + request.getCode() + "' already exists in this category");
        }

        com.secondhand.coreservice.model.enums.AttributeDataType dataTypeEnum;
        try {
            dataTypeEnum = com.secondhand.coreservice.model.enums.AttributeDataType.valueOf(request.getDataType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid data type: " + request.getDataType());
        }

        attribute.setCode(request.getCode());
        attribute.setName(request.getName());
        attribute.setDescription(request.getDescription());
        attribute.setDataType(dataTypeEnum);
        attribute.setUnit(request.getUnit());
        attribute.setRequired(request.getRequired());
        attribute.setFilterable(request.getFilterable());
        attribute.setSearchable(request.getSearchable());
        attribute.setMinValueNumber(request.getMinValueNumber());
        attribute.setMaxValueNumber(request.getMaxValueNumber());
        attribute.setOptionsJson(request.getOptionsJson());
        if (request.getSortOrder() != null) {
            attribute.setSortOrder(request.getSortOrder());
        }
        attribute.setUpdatedAt(LocalDateTime.now());

        CategoryAttribute savedAttribute = categoryAttributeRepository.save(attribute);
        return mapToCategoryAttributeResponse(savedAttribute);
    }

    @Override
    @CacheEvict(cacheNames = "categoryAttributes", key = "#categoryId")
    public MessageResponse deleteCategoryAttribute(String categoryId, String attributeId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }

        CategoryAttribute attribute = categoryAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute not found with id: " + attributeId));

        if (!attribute.getCategory().getCategoryId().equals(categoryId)) {
            throw new BadRequestException("Attribute does not belong to the specified category");
        }

        categoryAttributeRepository.delete(attribute);
        return new MessageResponse("Category attribute deleted successfully", true);
    }
}
