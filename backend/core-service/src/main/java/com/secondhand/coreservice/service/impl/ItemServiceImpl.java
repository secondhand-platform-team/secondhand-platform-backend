package com.secondhand.coreservice.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.secondhand.coreservice.dto.request.ItemImageRequest;
import com.secondhand.coreservice.dto.request.ItemRequest;
import com.secondhand.coreservice.dto.response.ItemImageResponse;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.LocationResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.exception.ResourceNotFoundException;
import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.ItemImage;
import com.secondhand.coreservice.model.Location;
import com.secondhand.coreservice.model.enums.ItemCondition;
import com.secondhand.coreservice.model.enums.ItemStatus;
import com.secondhand.coreservice.model.enums.TransactionType;
import com.secondhand.coreservice.repository.CategoryRepository;
import com.secondhand.coreservice.repository.ItemImageRepository;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.repository.LocationRepository;
import com.secondhand.coreservice.service.ItemService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ItemImageRepository itemImageRepository;

    @Override
    public ItemResponse createItem(ItemRequest request) {
        // Validate category exists
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new BadRequestException("Category not found with id: " + request.getCategoryId());
        }

        if (request.getPrice() == null || request.getPrice().signum() <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        String itemId = UUID.randomUUID().toString();
        Item item = Item.builder()
                .itemId(itemId)
                .title(request.getTitle())
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .price(request.getPrice())
                .condition(request.getCondition() != null ? ItemCondition.valueOf(request.getCondition()) : null)
                .transactionType(request.getTransactionType() != null ? TransactionType.valueOf(request.getTransactionType()) : null)
                .status(request.getStatus() != null ? ItemStatus.valueOf(request.getStatus()) : ItemStatus.AVAILABLE)
                .userId(request.getUserId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Create and set location if provided
        if (request.getLocation() != null) {
            Location location = new Location();
            location.setLocationId(UUID.randomUUID().toString());
            location.setAddress(request.getLocation().getAddress());
            location.setWard(request.getLocation().getWard());
            location.setDistrict(request.getLocation().getDistrict());
            location.setCity(request.getLocation().getCity());
            location.setItem(item);
            item.setItemLocation(location);
        }

        // Create and set images if provided
        if (request.getItemImageList() != null && !request.getItemImageList().isEmpty()) {
            List<ItemImage> images = new ArrayList<>();
            for (ItemImageRequest imageRequest : request.getItemImageList()) {
                ItemImage image = new ItemImage();
                image.setItem(item);
                image.setUrl(imageRequest.getImageUrl());
                image.setIsPrimary(imageRequest.getIsPrimary() != null ? imageRequest.getIsPrimary() : false);
                images.add(image);
            }
            item.setItemImageList(images);
        }

        Item savedItem = itemRepository.save(item);
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
        List<Item> items = itemRepository.findByCategoryId(categoryId);
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
        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new BadRequestException("Category not found with id: " + request.getCategoryId());
        }

        if (request.getPrice() != null && request.getPrice().signum() <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategoryId(request.getCategoryId());
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
                location.setLocationId(UUID.randomUUID().toString());
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

        Item updatedItem = itemRepository.save(item);
        return mapToItemResponse(updatedItem);
    }

    @Override
    public MessageResponse deleteItem(String itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }
        itemRepository.deleteById(itemId);
        return new MessageResponse("Item deleted successfully", true);
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

        return ItemResponse.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .description(item.getDescription())
                .categoryId(item.getCategoryId())
                .price(item.getPrice())
                .condition(item.getCondition() != null ? item.getCondition().name() : null)
                .transactionType(item.getTransactionType() != null ? item.getTransactionType().name() : null)
                .status(item.getStatus() != null ? item.getStatus().name() : null)
                .location(locationResponse)
                .userId(item.getUserId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .itemImageList(imageResponses)
                .build();
    }
}
