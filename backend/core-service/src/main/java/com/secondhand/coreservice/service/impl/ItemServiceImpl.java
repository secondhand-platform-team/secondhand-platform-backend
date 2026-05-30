package com.secondhand.coreservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondhand.coreservice.client.UserServiceClient;
import com.secondhand.coreservice.dto.request.ItemAttributeRequest;
import com.secondhand.coreservice.dto.request.ItemImageRequest;
import com.secondhand.coreservice.dto.request.ItemRequest;
import com.secondhand.coreservice.dto.request.RenewRequest;
import com.secondhand.coreservice.dto.request.VNPayCallbackRequest;
import com.secondhand.coreservice.dto.response.ItemAttributeResponse;
import com.secondhand.coreservice.dto.response.ItemImageResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.LocationResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.exception.ResourceNotFoundException;
import com.secondhand.coreservice.model.Category;
import com.secondhand.coreservice.model.CategoryAttribute;
import com.secondhand.coreservice.model.FavoriteItem;
import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.ItemAttributeValue;
import com.secondhand.coreservice.model.ItemImage;
import com.secondhand.coreservice.model.Location;
import com.secondhand.coreservice.model.enums.AttributeDataType;
import com.secondhand.coreservice.model.enums.ItemCondition;
import com.secondhand.coreservice.model.enums.ItemStatus;
import com.secondhand.coreservice.model.enums.TransactionType;
import com.secondhand.coreservice.repository.CategoryAttributeRepository;
import com.secondhand.coreservice.repository.CategoryRepository;
import com.secondhand.coreservice.repository.FavoriteItemRepository;
import com.secondhand.coreservice.repository.ItemAttributeValueRepository;
import com.secondhand.coreservice.repository.ItemImageRepository;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.repository.LocationRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.CloudinaryService;
import com.secondhand.coreservice.service.ItemService;
import com.secondhand.coreservice.service.PaymentEventService;
import com.secondhand.coreservice.client.PaymentRestClient.PaymentCreateResult;
import com.secondhand.coreservice.client.PaymentRestClient.PaymentVerifyResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ItemAttributeValueRepository itemAttributeValueRepository;
    private final FavoriteItemRepository favoriteItemRepository;
    private final LocationRepository locationRepository;
    private final ItemImageRepository itemImageRepository;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;
    private final UserServiceClient userServiceClient;
    private final PaymentEventService paymentEventService;
    private final com.secondhand.coreservice.service.WalletService walletService;
    private final com.secondhand.coreservice.service.NotificationService notificationService;

    @Value("${app.payment.item-callback-url}")
    private String itemCallbackUrl;

    @Value("${app.payment.item-renew-callback-url}")
    private String itemRenewCallbackUrl;

    // Thời hạn tin đăng
    private static final int FREE_LISTING_DAYS = 5;   // FREE_SELL / GIVE_AWAY
    private static final int PAID_LISTING_DAYS = 15;  // SELL (tính phí)

    @Override
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public ItemResponse createItem(ItemRequest request) {
        return createItem(request, null);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public ItemResponse createItem(ItemRequest request, MultipartFile[] images) {
        log.info("Creating item: {} with {} images", request.getTitle(), images != null ? images.length : 0);

        // Handle image uploads if provided
        if (images != null && images.length > 0) {
            processAndUploadImages(request, images);
        }

        return createItemInternal(request);
    }

    /**
     * Process and upload images to Cloudinary
     */
    private void processAndUploadImages(ItemRequest request, MultipartFile[] images) {
        List<ItemImageRequest> imageRequests = new ArrayList<>();

        for (int i = 0; i < images.length; i++) {
            MultipartFile file = images[i];

            // Validate file
            if (file.isEmpty()) {
                log.warn("Skipping empty file at index {}", i);
                continue;
            }

            if (!isValidImageFile(file)) {
                throw new BadRequestException(
                        "Invalid image file at index " + i + ". Allowed types: jpg, jpeg, png, gif, webp");
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                throw new BadRequestException("Image file at index " + i + " exceeds 10MB limit");
            }

            try {
                String imageUrl = cloudinaryService.uploadImage(file);
                ItemImageRequest imageRequest = ItemImageRequest.builder()
                        .imageUrl(imageUrl)
                        .isPrimary(i == 0) // First image is primary
                        .build();
                imageRequests.add(imageRequest);
                log.debug("Successfully uploaded image {} to: {}", i, imageUrl);
            } catch (Exception e) {
                log.error("Failed to upload image at index {}", i, e);
                throw new BadRequestException("Failed to upload image: " + e.getMessage());
            }
        }

        if (!imageRequests.isEmpty()) {
            request.setItemImageList(imageRequests);
            log.info("Successfully processed {} images", imageRequests.size());
        }
    }

    /**
     * Validate image file type
     */
    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/") &&
                (contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/webp"));
    }

    /**
     * Internal item creation logic
     */
    private ItemResponse createItemInternal(ItemRequest request) {
        log.debug("Starting internal item creation process");

        // Lấy userId từ JWT token (SecurityContext)
        String userId = getCurrentUserId();

        // Determine if this is a SELL, GIVE_AWAY, or FREE_SELL transaction
        String transactionType = request.getTransactionType();
        boolean isGiveAway = transactionType != null && "GIVE_AWAY".equalsIgnoreCase(transactionType);
        boolean isFreeSell = transactionType != null && "FREE_SELL".equalsIgnoreCase(transactionType);
        boolean isSell = transactionType != null && "SELL".equalsIgnoreCase(transactionType);

        // For SELL items, check if user can post for free or needs payment
        if (isSell) {
            // Check if user has free sell slots available
            try {
                int freeSellUsed = userServiceClient.getFreeSellUsed(userId);

                if (freeSellUsed > 0) {
                    // User can post for free
                    log.info("User {} has {} free sell uses available, allowing free posting", userId, freeSellUsed);
                    // Change transaction type to FREE_SELL so item will be ACTIVE directly
                    transactionType = "FREE_SELL";
                    isFreeSell = true;
                    isSell = false;
                } else {
                    // User has no free slots, must pay
                    log.info("User {} has no free sell uses, payment verification required", userId);
                    verifyPaymentBeforeCreatingItem(request);
                }
            } catch (Exception e) {
                log.error("Error checking free sell uses for user: {}", userId, e);
                // Fall back to payment verification if we can't check free uses
                verifyPaymentBeforeCreatingItem(request);
            }
        }

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Category not found with id: " + request.getCategoryId()));

        // Validate price based on transaction type
        if (request.getPrice() == null) {
            throw new BadRequestException("Price is required");
        }

        if (isGiveAway || isFreeSell) {
            // GIVE_AWAY/FREE_SELL: price can be 0 or any positive value
            if (request.getPrice().signum() < 0) {
                throw new BadRequestException("Price cannot be negative");
            }
        } else if (isSell) {
            // SELL: price must be greater than 0
            if (request.getPrice().signum() <= 0) {
                throw new BadRequestException("Price must be greater than 0 for SELL items");
            }
        }

        // Determine initial status based on transaction type
        // GIVE_AWAY/FREE_SELL: Direct ACTIVE (no payment needed)
        // SELL: DRAFT (waiting for payment)
        ItemStatus initialStatus = (isGiveAway || isFreeSell) ? ItemStatus.ACTIVE : ItemStatus.DRAFT;

        // Set expiredAt cho tin miễn phí ngay khi tạo (ACTIVE luôn)
        LocalDateTime expiredAt = null;
        if (isGiveAway || isFreeSell) {
            expiredAt = LocalDateTime.now().plusDays(FREE_LISTING_DAYS);
        }

        // Create item
        Item item = Item.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .price(request.getPrice())
                .condition(request.getCondition() != null ? ItemCondition.valueOf(request.getCondition()) : null)
                .transactionType(
                        transactionType != null ? TransactionType.valueOf(transactionType)
                                : null)
                .status(initialStatus)
                .userId(userId)
                .expiredAt(expiredAt)
                .paymentInitiatedAt((isSell) ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create and set location if provided
        if (request.getLocation() != null) {
            Location location = new Location();
            location.setAddress(request.getLocation().getAddress());
            location.setWard(request.getLocation().getWard());
            location.setDistrict(request.getLocation().getDistrict());
            location.setCity(request.getLocation().getCity());
            location.setItem(item);
            item.setItemLocation(location);
        }

        // Create and set images if provided
        if (request.getItemImageList() != null && !request.getItemImageList().isEmpty()) {
            List<ItemImage> itemImages = new ArrayList<>();
            for (ItemImageRequest imageRequest : request.getItemImageList()) {
                ItemImage image = new ItemImage();
                image.setItem(item);
                image.setUrl(imageRequest.getImageUrl());
                image.setIsPrimary(imageRequest.getIsPrimary() != null ? imageRequest.getIsPrimary() : false);
                itemImages.add(image);
            }
            item.setItemImageList(itemImages);
        }

        // Save item
        Item savedItem = itemRepository.save(item);
        log.info("Item created with {} status and id: {}", initialStatus, savedItem.getItemId());

        // Process and save attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            List<ItemAttributeValue> attributeValues = new ArrayList<>();
            for (ItemAttributeRequest attrRequest : request.getAttributes()) {
                // Reload attribute fresh từ DB để tránh Hibernate session issue
                CategoryAttribute categoryAttr = categoryAttributeRepository
                        .findByCategory_CategoryIdAndCode(category.getCategoryId(), attrRequest.getCode())
                        .orElseThrow(() -> new BadRequestException(
                                "Attribute with code '" + attrRequest.getCode() + "' not found in category"));

                // Validate required attribute
                if (categoryAttr.getRequired() && attrRequest.getValue() == null) {
                    throw new BadRequestException("Attribute '" + attrRequest.getCode() + "' is required");
                }

                if (attrRequest.getValue() != null) {
                    ItemAttributeValue attrValue = buildAttributeValue(
                            savedItem, categoryAttr, attrRequest.getValue());
                    // Save từng attribute value làm cho giảm conflict
                    attrValue = itemAttributeValueRepository.save(attrValue);
                    attributeValues.add(attrValue);
                }
            }
            savedItem.setAttributeValues(attributeValues);
        }

        // Handle FREE_SELL: decrement free sell uses
        if (isFreeSell) {
            try {
                log.info("Decrementing free sell use for user: {}", userId);
                userServiceClient.decrementFreeSellUse(userId);
                log.info("Free sell use decremented successfully for user: {}", userId);
            } catch (Exception e) {
                log.error("Error decrementing free sell use for user: {}", userId, e);
                // Continue anyway - item was already created
            }
        }

        // Only create payment for SELL items (not FREE_SELL or GIVE_AWAY)
        if (isSell) {
            String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "VNPAY";
            
            java.math.BigDecimal fee = request.getPostingFee();
            if (fee == null) {
                throw new BadRequestException("Posting fee is required for SELL items");
            }

            if ("WALLET".equals(paymentMethod)) {
                log.info("Paying for SELL item: {} with WALLET userId: {}", savedItem.getItemId(), userId);
                try {
                    // Trừ tiền trong ví theo postingFee
                    walletService.deductFee(userId, fee, "Thanh toán đăng tin " + savedItem.getItemId());

                    // Cập nhật trạng thái item thành ACTIVE vì đã thanh toán thành công bằng ví
                    savedItem.setStatus(ItemStatus.ACTIVE);
                    savedItem.setTransactionId("WALLET-" + System.currentTimeMillis());
                    savedItem.setExpiredAt(LocalDateTime.now().plusDays(PAID_LISTING_DAYS));
                    savedItem.setUpdatedAt(LocalDateTime.now());
                    itemRepository.save(savedItem);
                    log.info("Wallet payment successful, item {} is now ACTIVE", savedItem.getItemId());

                    // Send notification for successful posting via wallet
                    notificationService.createAndSendNotification(
                            userId,
                            "Chúc mừng! Tin đăng \"" + savedItem.getTitle() + "\" của bạn đã được duyệt thành công.",
                            com.secondhand.coreservice.model.enums.NotificationType.SYSTEM,
                            savedItem.getItemId());
                } catch (Exception e) {
                    log.error("Failed to pay with WALLET: {}", e.getMessage());
                    itemRepository.delete(savedItem);
                    throw new BadRequestException("Wallet payment failed: " + e.getMessage());
                }
            } else {
                try {
                    log.info("Creating external payment link for SELL item: {} with userId: {}, method: {}",
                            savedItem.getItemId(), userId, paymentMethod);

                    PaymentCreateResult paymentResponse = paymentEventService
                            .createVnPayPayment(
                                    fee.longValue(),
                                    "NCB",
                                    "vn",
                                    userId,
                                    itemCallbackUrl);

                    if ("00".equals(paymentResponse.code())) {
                        // Store transaction ID and payment URL in item for tracking
                        savedItem.setTransactionId(paymentResponse.transactionId());
                        savedItem.setPaymentUrl(paymentResponse.paymentUrl());
                        itemRepository.save(savedItem);
                        log.info("Payment created successfully - TransactionId: {}", paymentResponse.transactionId());
                    } else {
                        log.error("Failed to create payment: {}", paymentResponse.message());
                        throw new BadRequestException("Failed to create payment: " + paymentResponse.message());
                    }
                } catch (Exception e) {
                    log.error("Error creating payment for item", e);
                    // Delete the draft item if payment creation fails
                    itemRepository.delete(savedItem);
                    throw new BadRequestException("Failed to create payment: " + e.getMessage());
                }
            }
        } else {
            log.info("Item created directly as ACTIVE - no payment needed (type: {})", transactionType);
            // Send notification for successful posting (GIVE_AWAY or FREE_SELL)
            notificationService.createAndSendNotification(
                    userId,
                    "Chúc mừng! Tin đăng \"" + savedItem.getTitle() + "\" của bạn đã được duyệt thành công.",
                    com.secondhand.coreservice.model.enums.NotificationType.SYSTEM,
                    savedItem.getItemId());
        }

        return mapToItemResponse(savedItem);
    }

    @Override
    @Transactional
    public ItemResponse getItemById(String itemId) {
        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Prevent access to soft-deleted items
        if (item.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }
        
        // Read-repair: Cập nhật DB ngay lập tức nếu phát hiện hết hạn (thay vì đợi scheduler)
        readRepairExpiredItems(Arrays.asList(item));

        return mapToItemResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems() {
        List<Item> items = itemRepository.findAllNotDeleted();
        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponse> getAllItemsPaginated(int page, int size, String sort) {
        Sort sortOrder = "oldest".equals(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Item> items = itemRepository.findAllByStatus(ItemStatus.ACTIVE, pageable);
        return items.map(this::mapToItemResponse);
    }

    @Override
    @Transactional
    public List<ItemResponse> getMyItems() {
        String currentUserId = getCurrentUserId();
        List<Item> items = itemRepository.findByUserId(currentUserId);
        
        // Read-repair
        readRepairExpiredItems(items);
        
        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Page<ItemResponse> getMyItemsPaginated(int page, int size) {
        String currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Item> items = itemRepository.findByUserId(currentUserId, pageable);
        
        // Read-repair
        readRepairExpiredItems(items.getContent());
        
        return items.map(this::mapToItemResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponse> searchItems(
            String keyword,
            String categoryId,
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
        String sortParam = (sort != null && !sort.isBlank()) ? sort : "newest";
        Pageable pageable = PageRequest.of(page, size);
        String keywordParam = normalizeKeywordPattern(keyword);
        String categoryIdParam = (categoryId != null && !categoryId.isBlank()) ? categoryId : null;
        String cityParam = normalizeCityPattern(city);
        String districtParam = normalizeDistrictPattern(district);
        String wardParam = normalizeWardPattern(ward);

        ItemCondition conditionParam = null;
        if (condition != null && !condition.isBlank()) {
            try {
                conditionParam = ItemCondition.valueOf(condition.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        TransactionType transactionTypeParam = null;
        if (transactionType != null && !transactionType.isBlank()) {
            try {
                transactionTypeParam = TransactionType.valueOf(transactionType.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<Item> items = itemRepository.searchItems(
                keywordParam, categoryIdParam, minPrice, maxPrice,
                conditionParam != null ? conditionParam.name() : null,
                transactionTypeParam != null ? transactionTypeParam.name() : null,
                cityParam, districtParam, wardParam, sortParam, pageable);
        return items.map(this::mapToItemResponse);
    }

    private String normalizeKeywordPattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", "%");
        return "%" + normalized + "%";
    }

    private String normalizeCityPattern(String city) {
        return normalizeLocationPattern(city,
                "thành phố ",
                "tp. ",
                "tp ",
                "tỉnh ");
    }

    private String normalizeDistrictPattern(String district) {
        return normalizeLocationPattern(district,
                "quận ",
                "huyện ",
                "thị xã ",
                "tx. ",
                "tx ",
                "thành phố ",
                "tp. ",
                "tp ");
    }

    private String normalizeWardPattern(String ward) {
        return normalizeLocationPattern(ward,
                "phường ",
                "xã ",
                "thị trấn ");
    }

    private String normalizeLocationPattern(String value, String... prefixes) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        String lower = normalized.toLowerCase();

        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length()).trim();
                break;
            }
        }

        if (normalized.isBlank()) {
            return null;
        }

        normalized = normalized.replaceAll("\\s+", "%");
        return "%" + normalized + "%";
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getFeaturedItems(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        List<Item> items = itemRepository.findTopActiveItems(pageable);
        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByCategory(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        List<Item> items = itemRepository.findByCategory_CategoryId(categoryId);
        log.info("Found {} items in category {}", items.size(), categoryId);
        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByCategorySlug(String slug) {
        if (!categoryRepository.existsBySlug(slug)) {
            throw new ResourceNotFoundException("Category not found with slug: " + slug);
        }

        return itemRepository.findByCategory_SlugAndStatusIn(
                slug,
                Arrays.asList(ItemStatus.ACTIVE)).stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ItemResponse> getItemsByUser(String userId) {
        List<Item> items = itemRepository.findByUserId(userId);
        
        // Read-repair
        readRepairExpiredItems(items);
        
        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public ItemResponse updateItem(String itemId, ItemRequest request) {
        String currentUserId = getCurrentUserId();

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Validate user is the owner of this item
        if (!item.getUserId().equals(currentUserId)) {
            throw new BadRequestException("You do not have permission to update this item");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Category not found with id: " + request.getCategoryId()));

        if (request.getPrice() != null && request.getPrice().signum() <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(category);
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getCondition() != null) {
            item.setCondition(ItemCondition.valueOf(request.getCondition()));
        }
        if (request.getTransactionType() != null) {
            item.setTransactionType(TransactionType.valueOf(request.getTransactionType()));
        }
        if (request.getStatus() != null) {
            item.setStatus(ItemStatus.valueOf(request.getStatus()));
        }
        item.setUpdatedAt(LocalDateTime.now());

        // Update location if provided
        if (request.getLocation() != null) {
            if (item.getItemLocation() != null) {
                Location location = item.getItemLocation();
                location.setAddress(request.getLocation().getAddress());
                location.setWard(request.getLocation().getWard());
                location.setDistrict(request.getLocation().getDistrict());
                location.setCity(request.getLocation().getCity());
                locationRepository.save(location);
            } else {
                Location location = new Location();
                location.setAddress(request.getLocation().getAddress());
                location.setWard(request.getLocation().getWard());
                location.setDistrict(request.getLocation().getDistrict());
                location.setCity(request.getLocation().getCity());
                location.setItem(item);
                locationRepository.save(location);
                item.setItemLocation(location);
            }
        }

        // Update images if provided
        if (request.getItemImageList() != null) {
            if (item.getItemImageList() != null) {
                itemImageRepository.deleteAll(item.getItemImageList());
            }
            List<ItemImage> images = new ArrayList<>();
            for (ItemImageRequest imageRequest : request.getItemImageList()) {
                ItemImage image = new ItemImage();
                image.setItem(item);
                image.setUrl(imageRequest.getImageUrl());
                image.setIsPrimary(imageRequest.getIsPrimary() != null ? imageRequest.getIsPrimary() : false);
                images.add(image);
            }
            itemImageRepository.saveAll(images);
            item.setItemImageList(images);
        }

        // Update attributes if provided
        if (request.getAttributes() != null) {
            itemAttributeValueRepository.deleteByItem_ItemId(itemId);
            List<ItemAttributeValue> attributeValues = new ArrayList<>();
            for (ItemAttributeRequest attrRequest : request.getAttributes()) {
                CategoryAttribute categoryAttr = categoryAttributeRepository
                        .findByCategory_CategoryIdAndCode(category.getCategoryId(), attrRequest.getCode())
                        .orElseThrow(() -> new BadRequestException(
                                "Attribute with code '" + attrRequest.getCode() + "' not found in category"));

                if (categoryAttr.getRequired() && attrRequest.getValue() == null) {
                    throw new BadRequestException("Attribute '" + attrRequest.getCode() + "' is required");
                }

                if (attrRequest.getValue() != null) {
                    ItemAttributeValue attrValue = buildAttributeValue(
                            item, categoryAttr, attrRequest.getValue());
                    attributeValues.add(attrValue);
                }
            }
            itemAttributeValueRepository.saveAll(attributeValues);
            item.setAttributeValues(attributeValues);
        }

        Item updatedItem = itemRepository.save(item);
        return mapToItemResponse(updatedItem);
    }

    @Override
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public ItemResponse updateItemStatus(String itemId, String status) {
        String currentUserId = getCurrentUserId();

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        if (!item.getUserId().equals(currentUserId) && !isCurrentUserAdmin()) {
            throw new BadRequestException("You do not have permission to update this item status");
        }

        ItemStatus itemStatus;
        try {
            itemStatus = ItemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid status. Allowed values: AVAILABLE, RESERVED, SOLD, HIDDEN, ACTIVE");
        }

        item.setStatus(itemStatus);
        item.setUpdatedAt(LocalDateTime.now());

        Item updatedItem = itemRepository.save(item);
        return mapToItemResponse(updatedItem);
    }

    @Override
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public MessageResponse deleteItem(String itemId) {
        String currentUserId = getCurrentUserId();

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Validate user is the owner of this item
        // if (!item.getUserId().equals(currentUserId)) {
        // throw new BadRequestException("You do not have permission to delete this
        // item");
        // }

        // Soft delete: mark as deleted instead of removing from database
        item.setDeletedAt(java.time.LocalDateTime.now());
        item.setUpdatedAt(java.time.LocalDateTime.now());
        itemRepository.save(item);

        return new MessageResponse("Item deleted successfully", true);
    }

    @Override
    public MessageResponse addFavoriteItem(String itemId) {
        String currentUserId = getCurrentUserId();

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        if (favoriteItemRepository.existsByUserIdAndItem_ItemId(currentUserId, itemId)) {
            return new MessageResponse("Item already in favorites", true);
        }

        FavoriteItem favoriteItem = new FavoriteItem();
        favoriteItem.setUserId(currentUserId);
        favoriteItem.setItem(item);
        favoriteItemRepository.save(favoriteItem);

        return new MessageResponse("Item added to favorites", true);
    }

    @Override
    public MessageResponse removeFavoriteItem(String itemId) {
        String currentUserId = getCurrentUserId();

        FavoriteItem favoriteItem = favoriteItemRepository.findByUserIdAndItem_ItemId(currentUserId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite item not found"));

        favoriteItemRepository.delete(favoriteItem);
        return new MessageResponse("Item removed from favorites", true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getMyFavoriteItems() {
        String currentUserId = getCurrentUserId();

        return favoriteItemRepository.findByUserId(currentUserId)
                .stream()
                .map(FavoriteItem::getItem)
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ItemResponse createItemWithFileUpload(String itemJsonString, MultipartFile[] images) {
        try {
            // Parse JSON string to ItemRequest
            ItemRequest request = objectMapper.readValue(itemJsonString, ItemRequest.class);
            // Delegate to main createItem with file upload handling
            return createItem(request, images);
        } catch (Exception e) {
            log.error("Failed to parse item JSON or process upload", e);
            throw new BadRequestException("Failed to process item with images: " + e.getMessage());
        }
    }

    /**
     * Build ItemAttributeValue từ request, convert value về đúng type
     */
    private ItemAttributeValue buildAttributeValue(Item item, CategoryAttribute attribute, Object value) {
        ItemAttributeValue attrValue = new ItemAttributeValue();
        attrValue.setItem(item);
        attrValue.setAttribute(attribute);
        attrValue.setCreatedAt(LocalDateTime.now());
        attrValue.setUpdatedAt(LocalDateTime.now());

        // Convert value theo attribute dataType
        AttributeDataType dataType = attribute.getDataType();
        try {
            switch (dataType) {
                case STRING:
                    attrValue.setValueString(value.toString());
                    break;
                case NUMBER:
                    attrValue.setValueNumber(new BigDecimal(value.toString()));
                    break;
                case INTEGER:
                    attrValue.setValueInteger(Long.parseLong(value.toString()));
                    break;
                case BOOLEAN:
                    attrValue.setValueBoolean(Boolean.parseBoolean(value.toString()));
                    break;
                case DATE:
                    attrValue.setValueDate(LocalDate.parse(value.toString()));
                    break;
                case ENUM:
                    attrValue.setValueString(value.toString());
                    break;
                case JSON:
                    attrValue.setValueJson(objectMapper.writeValueAsString(value));
                    break;
                default:
                    throw new BadRequestException("Unknown attribute data type: " + dataType);
            }
        } catch (Exception e) {
            throw new BadRequestException(
                    "Invalid value '" + value + "' for attribute type " + dataType + ": " + e.getMessage());
        }

        return attrValue;
    }

    /**
     * Map ItemAttributeValue thành ItemAttributeResponse
     */
    private ItemAttributeResponse mapAttributeToResponse(ItemAttributeValue attrValue) {
        Object value = null;
        switch (attrValue.getAttribute().getDataType()) {
            case STRING:
            case ENUM:
                value = attrValue.getValueString();
                break;
            case NUMBER:
                value = attrValue.getValueNumber();
                break;
            case INTEGER:
                value = attrValue.getValueInteger();
                break;
            case BOOLEAN:
                value = attrValue.getValueBoolean();
                break;
            case DATE:
                value = attrValue.getValueDate();
                break;
            case JSON:
                try {
                    value = objectMapper.readValue(attrValue.getValueJson(), Object.class);
                } catch (Exception e) {
                    value = attrValue.getValueJson();
                }
                break;
        }

        return ItemAttributeResponse.builder()
                .attributeId(attrValue.getAttribute().getAttributeId())
                .code(attrValue.getAttribute().getCode())
                .name(attrValue.getAttribute().getName())
                .description(attrValue.getAttribute().getDescription())
                .dataType(attrValue.getAttribute().getDataType().name())
                .unit(attrValue.getAttribute().getUnit())
                .value(value)
                .build();
    }

    /**
     * Read-Repair Pattern: Nếu người dùng truy cập vào item đã hết hạn (nhưng scheduler chưa chạy),
     * thì ta tự động đổi trạng thái sang HIDDEN và lưu xuống DB ngay lập tức.
     */
    private void readRepairExpiredItems(List<Item> items) {
        if (items == null || items.isEmpty()) return;
        
        boolean needSave = false;
        LocalDateTime now = LocalDateTime.now();
        for (Item item : items) {
            if (item.getStatus() == ItemStatus.ACTIVE 
                && item.getExpiredAt() != null 
                && item.getExpiredAt().isBefore(now)) {
                
                item.setStatus(ItemStatus.HIDDEN);
                item.setUpdatedAt(now);
                needSave = true;
                log.info("[Read-Repair] Đã tự động đổi trạng thái item {} sang HIDDEN vì đã quá hạn", item.getItemId());
            }
        }
        
        if (needSave) {
            itemRepository.saveAll(items);
        }
    }

    private ItemResponse mapToItemResponse(Item item) {
        LocationResponse locationResponse = null;
        if (item.getItemLocation() != null) {
            locationResponse = LocationResponse.builder()
                    .address(item.getItemLocation().getAddress())
                    .ward(item.getItemLocation().getWard())
                    .district(item.getItemLocation().getDistrict())
                    .city(item.getItemLocation().getCity())
                    .build();
        }

        List<ItemImageResponse> imageResponses = new ArrayList<>();
        if (item.getItemImageList() != null && !item.getItemImageList().isEmpty()) {
            imageResponses = item.getItemImageList().stream()
                    .map(img -> ItemImageResponse.builder()
                            .imageUrl(img.getUrl())
                            .isPrimary(img.getIsPrimary())
                            .build())
                    .collect(Collectors.toList());
        }

        List<ItemAttributeResponse> attributeResponses = new ArrayList<>();
        if (item.getAttributeValues() != null && !item.getAttributeValues().isEmpty()) {
            attributeResponses = item.getAttributeValues().stream()
                    .map(this::mapAttributeToResponse)
                    .collect(Collectors.toList());
        }

        long favCount = favoriteItemRepository.countByItem_ItemId(item.getItemId());
        boolean favedByMe = false;
        try {
            String currentUserId = getCurrentUserId();
            favedByMe = favoriteItemRepository.existsByUserIdAndItem_ItemId(currentUserId, item.getItemId());
        } catch (Exception e) {
            // Not authenticated or error getting user id
        }

        // Tính toán trạng thái thực tế hiển thị: 
        // Nếu DB là ACTIVE nhưng đã quá hạn -> ép FE hiển thị là HIDDEN ngay lập tức.
        String displayStatus = item.getStatus() != null ? item.getStatus().name() : null;
        if (item.getStatus() == ItemStatus.ACTIVE 
            && item.getExpiredAt() != null 
            && item.getExpiredAt().isBefore(LocalDateTime.now())) {
            displayStatus = ItemStatus.HIDDEN.name();
        }

        return ItemResponse.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .description(item.getDescription())
                .categoryId(item.getCategory().getCategoryId())
                .price(item.getPrice())
                .condition(item.getCondition() != null ? item.getCondition().name() : null)
                .transactionType(item.getTransactionType() != null ? item.getTransactionType().name() : null)
                .status(displayStatus)
                .location(locationResponse)
                .userId(item.getUserId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .expiredAt(item.getExpiredAt())
                .itemImageList(imageResponses)
                .attributes(attributeResponses)
                .transactionId(item.getTransactionId())
                .paymentUrl(item.getPaymentUrl())
                .isFavorited(favedByMe)
                .favoriteCount(favCount)
                .build();
    }

    /**
     * Verify payment before allowing item creation
     * If payment information is provided, it must be valid for the item to be
     * created
     */
    private void verifyPaymentBeforeCreatingItem(ItemRequest request) {
        // If payment fields are not provided, skip verification for free items
        if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            log.info("No payment information provided, skipping payment verification");
            return;
        }

        // All payment fields must be provided if transaction ID is provided
        if (request.getOrderId() == null || request.getOrderId().isBlank() ||
                request.getResponseCode() == null || request.getResponseCode().isBlank() ||
                request.getSecureHash() == null || request.getSecureHash().isBlank()) {
            throw new BadRequestException(
                    "Complete payment information is required (transactionId, orderId, responseCode, secureHash)");
        }

        log.info("Verifying payment for item - TransactionId: {}, OrderId: {}",
                request.getTransactionId(), request.getOrderId());

        try {
            // Verify payment through REST call to order-service
            PaymentVerifyResult paymentResponse = paymentEventService.verifyPaymentCallback(
                    request.getTransactionId(),
                    request.getOrderId(),
                    request.getResponseCode(),
                    request.getSecureHash());

            // Check if payment is valid
            if (!paymentResponse.valid()) {
                log.warn("Payment verification failed for transaction: {}", request.getTransactionId());
                throw new BadRequestException("Payment verification failed: " + paymentResponse.message());
            }

            log.info("Payment verified successfully for transaction: {}", request.getTransactionId());
        } catch (RuntimeException e) {
            log.error("Error during payment verification", e);
            throw new BadRequestException("Payment verification error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleVNPayCallback(VNPayCallbackRequest request) {
        try {
            log.info("Processing VNPay callback - TxnRef: {}, ResponseCode: {}",
                    request.getVnp_TxnRef(), request.getVnp_ResponseCode());

            // Verify response code (00 = success)
            if (!"00".equals(request.getVnp_ResponseCode())) {
                log.warn("VNPay callback with non-success response code: {}", request.getVnp_ResponseCode());
                return;
            }

            // Find item by transactionId (which contains TxnRef)
            List<Item> items = itemRepository.findAll();
            Item targetItem = items.stream()
                    .filter(item -> item.getTransactionId() != null &&
                            item.getTransactionId().contains(request.getVnp_TxnRef()))
                    .findFirst()
                    .orElse(null);

            if (targetItem == null) {
                log.warn("No item found with TxnRef: {}", request.getVnp_TxnRef());
                return;
            }

            // Update item status from DRAFT to ACTIVE
            targetItem.setStatus(ItemStatus.ACTIVE);
            targetItem.setExpiredAt(LocalDateTime.now().plusDays(PAID_LISTING_DAYS));
            targetItem.setUpdatedAt(LocalDateTime.now());
            itemRepository.save(targetItem);

            // Update payment status in order-service
            try {
                paymentEventService.updatePaymentStatus(targetItem.getTransactionId(), "PAID");
                log.info("Payment status updated to PAID for paymentId: {}", targetItem.getTransactionId());
            } catch (Exception ex) {
                log.warn("Failed to update payment status in order-service", ex);
            }

            log.info("Item {} activated after successful payment", targetItem.getItemId());

            // Send notification for successful posting after payment
            notificationService.createAndSendNotification(
                targetItem.getUserId(),
                "Chúc mừng! Tin đăng \"" + targetItem.getTitle() + "\" của bạn đã được duyệt thành công.",
                com.secondhand.coreservice.model.enums.NotificationType.SYSTEM,
                targetItem.getItemId()
            );
        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            throw new RuntimeException("Failed to process payment callback: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy userId từ JWT token (SecurityContext)
     */
    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticatedUser) {
            JwtAuthenticatedUser user = (JwtAuthenticatedUser) authentication.getPrincipal();
            return user.userId();
        }
        throw new BadRequestException("User not authenticated or invalid JWT token");
    }

    private boolean isCurrentUserAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        }
        return false;
    }

    // ====================================================================
    // Internal: cập nhật item status từ order-service (không cần auth)
    // ====================================================================

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public ItemResponse updateItemStatusInternal(String itemId, String status) {
        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        ItemStatus itemStatus;
        try {
            itemStatus = ItemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid status: " + status);
        }

        item.setStatus(itemStatus);
        item.setUpdatedAt(LocalDateTime.now());

        Item updatedItem = itemRepository.save(item);
        log.info("Item {} status updated internally to: {}", itemId, status);
        return mapToItemResponse(updatedItem);
    }

    // ====================================================================
    // Internal: Reserve item (atomic, SELECT FOR UPDATE)
    // Race Condition Prevention — chỉ 1 buyer có thể reserve item tại 1 thời điểm
    // ====================================================================

    // ====================================================================
    // Gia hạn tin đăng đã hết hạn
    // ====================================================================

    private ItemResponse processFreeRenewal(Item item, String currentUserId, String itemId) {
        item.setExpiredAt(LocalDateTime.now().plusDays(FREE_LISTING_DAYS));
        item.setStatus(ItemStatus.ACTIVE);
        item.setUpdatedAt(LocalDateTime.now());
        Item saved = itemRepository.save(item);
        log.info("[Renew] Gia hạn miễn phí {} thêm {} ngày", itemId, FREE_LISTING_DAYS);
        notificationService.createAndSendNotification(
                currentUserId,
                "Tin đăng \"" + item.getTitle() + "\" đã được gia hạn. Hạn mới: "
                        + saved.getExpiredAt().toLocalDate(),
                com.secondhand.coreservice.model.enums.NotificationType.SYSTEM, itemId);
        return mapToItemResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public ItemResponse renewItem(String itemId, RenewRequest request) {
        String currentUserId = getCurrentUserId();

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Chỉ chủ tin mới được gia hạn
        if (!item.getUserId().equals(currentUserId)) {
            throw new BadRequestException("Bạn không có quyền gia hạn tin đăng này");
        }

        // Chỉ gia hạn được khi tin đang HIDDEN
        if (item.getStatus() != ItemStatus.HIDDEN) {
            throw new BadRequestException(
                    "Chỉ có thể gia hạn tin đăng đang bị ẩn do hết hạn (trạng thái: " + item.getStatus() + ")");
        }

        TransactionType txType = item.getTransactionType();
        boolean isPaidListing = txType == TransactionType.SELL;

        // Xử lý riêng cho tin GIVE_AWAY: luôn miễn phí, không tốn lượt
        if (txType == TransactionType.GIVE_AWAY) {
            return processFreeRenewal(item, currentUserId, itemId);
        }

        // Xử lý cho tin FREE_SELL: phải kiểm tra còn lượt miễn phí không
        if (txType == TransactionType.FREE_SELL) {
            int freeSellAvailable = 0;
            try {
                freeSellAvailable = userServiceClient.getFreeSellUsed(currentUserId);
            } catch (Exception e) {
                log.error("[Renew] Lỗi khi lấy số lượt miễn phí của user {}", currentUserId, e);
            }

            if (freeSellAvailable > 0) {
                try {
                    userServiceClient.decrementFreeSellUse(currentUserId);
                } catch (Exception e) {
                    throw new BadRequestException("Lỗi hệ thống khi trừ lượt đăng miễn phí.");
                }
                log.info("[Renew] User {} dùng 1 lượt miễn phí (còn {}) để gia hạn item {}", currentUserId, freeSellAvailable - 1, itemId);
                return processFreeRenewal(item, currentUserId, itemId);
            } else {
                // Đã hết lượt miễn phí! Yêu cầu thanh toán.
                String requestedPayment = (request != null) ? request.getPaymentMethod() : null;
                if (requestedPayment == null || requestedPayment.isBlank()) {
                    throw new BadRequestException("Bạn đã hết lượt đăng miễn phí. Vui lòng chọn phương thức thanh toán (Ví hoặc VNPay) để tiếp tục gia hạn.");
                }
                
                // Nếu có chọn thanh toán, tự động nâng cấp tin này thành tin SELL (tính phí)
                item.setTransactionType(TransactionType.SELL);
                isPaidListing = true;
                log.info("[Renew] Nâng cấp item {} từ FREE_SELL -> SELL do hết lượt miễn phí", itemId);
            }
        }

        // Lúc này, nếu isPaidListing = false thì chỉ có thể là code logic sai, nhưng ta đã handle hết.
        // Đoạn dưới đây dành cho isPaidListing = true (hoặc SELL gốc, hoặc FREE_SELL bị nâng cấp lên SELL)

        // SELL: xác định phương thức thanh toán
        BigDecimal renewalFee = BigDecimal.valueOf(item.getCategory().getPostingFee());
        String paymentMethod = (request != null && request.getPaymentMethod() != null)
                ? request.getPaymentMethod().toUpperCase() : "WALLET";

        if ("WALLET".equals(paymentMethod)) {
            // ── Phương thức 1: Trừ phí từ ví ───────────────────────────────────────
            try {
                log.info("[Renew] Trừ phí gia hạn {} từ ví user {}", renewalFee, currentUserId);
                walletService.deductFee(currentUserId, renewalFee,
                        "Phí gia hạn tin đăng " + item.getItemId());
            } catch (Exception e) {
                log.error("[Renew] Trừ phí ví thất bại: {}", e.getMessage());
                throw new BadRequestException(
                        "Gia hạn bằng ví thất bại (có thể số dư không đủ): " + e.getMessage());
            }
            item.setExpiredAt(LocalDateTime.now().plusDays(PAID_LISTING_DAYS));
            item.setStatus(ItemStatus.ACTIVE);
            item.setUpdatedAt(LocalDateTime.now());
            Item saved = itemRepository.save(item);
            log.info("[Renew] Gia hạn SELL (WALLET) {} thêm {} ngày", itemId, PAID_LISTING_DAYS);
            notificationService.createAndSendNotification(
                    currentUserId,
                    "Tin đăng \"" + item.getTitle() + "\" đã được gia hạn. Hạn mới: "
                            + saved.getExpiredAt().toLocalDate(),
                    com.secondhand.coreservice.model.enums.NotificationType.SYSTEM, itemId);
            return mapToItemResponse(saved);

        } else if ("VNPAY".equals(paymentMethod)) {
            // ── Phương thức 2: Tạo link VNPay ───────────────────────────────────────
            // Item vẫn giữ HIDDEN, chỉ được ACTIVE sau khi VNPay callback thành công.
            try {
                log.info("[Renew] Tạo link VNPay gia hạn cho item {} user {}", itemId, currentUserId);
                PaymentCreateResult paymentResponse = paymentEventService.createVnPayPayment(
                        renewalFee.longValue(),
                        "NCB",
                        "vn",
                        currentUserId,
                        itemRenewCallbackUrl);

                if (!"00".equals(paymentResponse.code())) {
                    throw new BadRequestException(
                            "Tạo link thanh toán VNPay thất bại: " + paymentResponse.message());
                }

                // Lưu transactionId với prefix RENEW- để phân biệt khi callback
                item.setTransactionId("RENEW-" + paymentResponse.transactionId());
                item.setPaymentUrl(paymentResponse.paymentUrl());
                item.setPaymentInitiatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                itemRepository.save(item);

                log.info("[Renew] VNPay payment created - TransactionId: RENEW-{}",
                        paymentResponse.transactionId());
                return mapToItemResponse(item); // trả về paymentUrl để frontend redirect

            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.error("[Renew] Lỗi tạo VNPay payment: {}", e.getMessage(), e);
                throw new BadRequestException("Tạo link thanh toán thất bại: " + e.getMessage());
            }
        } else {
            throw new BadRequestException(
                    "Phương thức thanh toán không hợp lệ: " + paymentMethod + ". Dùng WALLET hoặc VNPAY.");
        }
    }

    // ====================================================================
    // VNPay callback cho gia hạn tin đăng
    // ====================================================================

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public void handleRenewVNPayCallback(VNPayCallbackRequest request) {
        try {
            log.info("[RenewCallback] VNPay callback - TxnRef: {}, ResponseCode: {}",
                    request.getVnp_TxnRef(), request.getVnp_ResponseCode());

            if (!"00".equals(request.getVnp_ResponseCode())) {
                log.warn("[RenewCallback] Thanh toán không thành công, ResponseCode: {}",
                        request.getVnp_ResponseCode());
                return;
            }

            // Tìm item theo transactionId chứa prefix RENEW- + TxnRef
            String renewTxnRef = "RENEW-" + request.getVnp_TxnRef();
            Item targetItem = itemRepository.findAll().stream()
                    .filter(i -> renewTxnRef.equals(i.getTransactionId())
                            || (i.getTransactionId() != null
                                && i.getTransactionId().contains(request.getVnp_TxnRef())
                                && i.getTransactionId().startsWith("RENEW-")))
                    .findFirst()
                    .orElse(null);

            if (targetItem == null) {
                log.warn("[RenewCallback] Không tìm thấy item cần gia hạn với TxnRef: {}",
                        request.getVnp_TxnRef());
                return;
            }

            // Kích hoạt lại item và reset expiredAt
            targetItem.setStatus(ItemStatus.ACTIVE);
            targetItem.setExpiredAt(LocalDateTime.now().plusDays(PAID_LISTING_DAYS));
            targetItem.setUpdatedAt(LocalDateTime.now());
            itemRepository.save(targetItem);

            log.info("[RenewCallback] Gia hạn thành công item {} (VNPay), expiredAt={}",
                    targetItem.getItemId(), targetItem.getExpiredAt());

            // Cập nhật trạng thái payment trong order-service
            try {
                paymentEventService.updatePaymentStatus(targetItem.getTransactionId(), "PAID");
            } catch (Exception ex) {
                log.warn("[RenewCallback] Không cập nhật được payment status: {}", ex.getMessage());
            }

            // Gửi thông báo gia hạn thành công
            notificationService.createAndSendNotification(
                    targetItem.getUserId(),
                    "Tin đăng \"" + targetItem.getTitle() + "\" đã được gia hạn thành công. Hạn mới: "
                            + targetItem.getExpiredAt().toLocalDate(),
                    com.secondhand.coreservice.model.enums.NotificationType.SYSTEM,
                    targetItem.getItemId());

        } catch (Exception e) {
            log.error("[RenewCallback] Lỗi xử lý callback gia hạn", e);
            throw new RuntimeException("Failed to process renewal callback: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"itemsAll", "itemsByCategory", "itemsByCategorySlug", "itemsFeatured"}, allEntries = true)
    public ItemResponse reserveItem(String itemId, String buyerId) {
        // SELECT FOR UPDATE — lock row, buyer thứ 2 phải chờ
        Item item = itemRepository.findByItemIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Check: chỉ ACTIVE mới được reserve
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new BadRequestException("Sản phẩm không còn khả dụng (trạng thái: " + item.getStatus() + ")");
        }

        // Reserve item
        item.setStatus(ItemStatus.RESERVED);
        item.setReservedBy(buyerId);
        item.setReservedUntil(LocalDateTime.now().plusMinutes(10)); // auto-release sau 10 phút nếu VNPay timeout
        item.setUpdatedAt(LocalDateTime.now());

        Item saved = itemRepository.save(item);
        log.info("Item {} reserved by buyer {} (expires at {})", itemId, buyerId, item.getReservedUntil());
        return mapToItemResponse(saved);
    }
}
