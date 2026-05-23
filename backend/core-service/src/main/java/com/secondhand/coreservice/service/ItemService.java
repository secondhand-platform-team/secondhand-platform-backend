package com.secondhand.coreservice.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.secondhand.coreservice.dto.request.ItemRequest;
import com.secondhand.coreservice.dto.request.VNPayCallbackRequest;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;

public interface ItemService {
    ItemResponse createItem(ItemRequest request);

    ItemResponse createItem(ItemRequest request, MultipartFile[] images);

    ItemResponse createItemWithFileUpload(String itemJsonString, MultipartFile[] images);

    ItemResponse getItemById(String itemId);

    List<ItemResponse> getAllItems();

    Page<ItemResponse> getAllItemsPaginated(int page, int size, String sort);

    List<ItemResponse> getMyItems();

    Page<ItemResponse> getMyItemsPaginated(int page, int size);

    List<ItemResponse> getItemsByCategory(String categoryId);

    List<ItemResponse> getItemsByCategorySlug(String slug);

    List<ItemResponse> getItemsByUser(String userId);

    Page<ItemResponse> searchItems(
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
            String sort);

    List<ItemResponse> getFeaturedItems(int limit);

    ItemResponse updateItem(String itemId, ItemRequest request);

    ItemResponse updateItemStatus(String itemId, String status);

    MessageResponse deleteItem(String itemId);

    MessageResponse addFavoriteItem(String itemId);

    MessageResponse removeFavoriteItem(String itemId);

    List<ItemResponse> getMyFavoriteItems();

    void handleVNPayCallback(VNPayCallbackRequest request);

    /** Internal: cập nhật status item từ order-service (không cần auth) */
    ItemResponse updateItemStatusInternal(String itemId, String status);
}
