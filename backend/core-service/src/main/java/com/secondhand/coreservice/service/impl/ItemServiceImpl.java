package com.secondhand.coreservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondhand.coreservice.client.UserServiceClient;
import com.secondhand.coreservice.dto.request.ItemAttributeRequest;
import com.secondhand.coreservice.dto.request.ItemImageRequest;
import com.secondhand.coreservice.dto.request.ItemRequest;
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
import com.secondhand.coreservice.repository.ItemAttributeValueRepository;
import com.secondhand.coreservice.repository.ItemImageRepository;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.repository.LocationRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.CloudinaryService;
import com.secondhand.coreservice.service.ItemService;
import com.secondhand.coreservice.service.PaymentEventService;
import com.secondhand.coreservice.grpc.payment.VerifyPaymentResponse;

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
    private final LocationRepository locationRepository;
    private final ItemImageRepository itemImageRepository;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;
    private final UserServiceClient userServiceClient;
    private final PaymentEventService paymentEventService;

    @Override
    public ItemResponse createItem(ItemRequest request) {
        return createItem(request, null);
    }

    @Override
    @Transactional
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

        // Determine if this is a SELL or GIVE_AWAY transaction
        boolean isGiveAway = request.getTransactionType() != null &&
                "GIVE_AWAY".equalsIgnoreCase(request.getTransactionType());

        // For SELL items, verify payment if provided
        if (!isGiveAway) {
            verifyPaymentBeforeCreatingItem(request);
        }

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BadRequestException("Category not found with id: " + request.getCategoryId()));

        // Validate price based on transaction type
        if (request.getPrice() == null) {
            throw new BadRequestException("Price is required");
        }

        if (isGiveAway) {
            // GIVE_AWAY: price can be 0 or any positive value
            if (request.getPrice().signum() < 0) {
                throw new BadRequestException("Price cannot be negative");
            }
        } else {
            // SELL: price must be greater than 0
            if (request.getPrice().signum() <= 0) {
                throw new BadRequestException("Price must be greater than 0 for SELL items");
            }
        }

        // Determine initial status based on transaction type
        // GIVE_AWAY: Direct ACTIVE (no payment needed)
        // SELL: DRAFT (waiting for payment)
        ItemStatus initialStatus = isGiveAway ? ItemStatus.ACTIVE : ItemStatus.DRAFT;

        // Create item
        Item item = Item.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(category)
                .price(request.getPrice())
                .condition(request.getCondition() != null ? ItemCondition.valueOf(request.getCondition()) : null)
                .transactionType(
                        request.getTransactionType() != null ? TransactionType.valueOf(request.getTransactionType())
                                : null)
                .status(initialStatus)
                .userId(userId)
                .paymentInitiatedAt(isGiveAway ? null : LocalDateTime.now())
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

        // Only create payment for SELL items
        if (!isGiveAway) {
            try {
                log.info("Creating payment for SELL item: {} with userId: {}", savedItem.getItemId(), userId);

                com.secondhand.coreservice.grpc.payment.CreatePaymentResponse paymentResponse = paymentEventService
                        .createVnPayPayment(
                                request.getPrice().longValue(),
                                "NCB",
                                "vn",
                                userId);

                if ("00".equals(paymentResponse.getCode())) {
                    // Store transaction ID and payment URL in item for tracking
                    savedItem.setTransactionId(paymentResponse.getTransactionId());
                    savedItem.setPaymentUrl(paymentResponse.getPaymentUrl());
                    itemRepository.save(savedItem);
                    log.info("Payment created successfully - TransactionId: {}", paymentResponse.getTransactionId());
                } else {
                    log.error("Failed to create payment: {}", paymentResponse.getMessage());
                    throw new BadRequestException("Failed to create payment: " + paymentResponse.getMessage());
                }
            } catch (Exception e) {
                log.error("Error creating payment for item", e);
                // Delete the draft item if payment creation fails
                itemRepository.delete(savedItem);
                throw new BadRequestException("Failed to create payment: " + e.getMessage());
            }
        } else {
            log.info("GIVE_AWAY item created directly as ACTIVE - no payment needed");
        }

        return mapToItemResponse(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponse getItemById(String itemId) {
        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));
        return mapToItemResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems() {
        List<Item> items = itemRepository.findAll();
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
        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByUser(String userId) {
        List<Item> items = itemRepository.findByUserId(userId);
        return items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());
    }

    @Override
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
    public MessageResponse deleteItem(String itemId) {
        String currentUserId = getCurrentUserId();

        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Validate user is the owner of this item
        if (!item.getUserId().equals(currentUserId)) {
            throw new BadRequestException("You do not have permission to delete this item");
        }

        itemRepository.deleteById(itemId);
        return new MessageResponse("Item deleted successfully", true);
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

        return ItemResponse.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .description(item.getDescription())
                .categoryId(item.getCategory().getCategoryId())
                .price(item.getPrice())
                .condition(item.getCondition() != null ? item.getCondition().name() : null)
                .transactionType(item.getTransactionType() != null ? item.getTransactionType().name() : null)
                .status(item.getStatus() != null ? item.getStatus().name() : null)
                .location(locationResponse)
                .userId(item.getUserId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .itemImageList(imageResponses)
                .attributes(attributeResponses)
                .transactionId(item.getTransactionId())
                .paymentUrl(item.getPaymentUrl())
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
            // Verify payment through gRPC call to order-service
            VerifyPaymentResponse paymentResponse = paymentEventService.verifyPaymentCallback(
                    request.getTransactionId(),
                    request.getOrderId(),
                    request.getResponseCode(),
                    request.getSecureHash());

            // Check if payment is valid
            if (!paymentResponse.getIsValid()) {
                log.warn("Payment verification failed for transaction: {}", request.getTransactionId());
                throw new BadRequestException("Payment verification failed: " + paymentResponse.getMessage());
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
            targetItem.setUpdatedAt(LocalDateTime.now());
            itemRepository.save(targetItem);

            // Update payment status in order-service
            try {
                paymentEventService.updatePaymentStatus(targetItem.getTransactionId(), "PAID");
                log.info("Payment status updated to PAID for paymentId: {}", targetItem.getTransactionId());
            } catch (Exception ex) {
                log.warn("Failed to update payment status in order-service", ex);
            }

            // Decrease free sell use count for the seller in auth-service
            try {
                userServiceClient.decrementFreeSellUse(targetItem.getUserId());
                log.info("Free sell use count decreased for user: {}", targetItem.getUserId());
            } catch (Exception ex) {
                log.warn("Failed to decrease free sell use count for user: {}", targetItem.getUserId(), ex);
            }

            log.info("Item {} activated after successful payment", targetItem.getItemId());
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
}
