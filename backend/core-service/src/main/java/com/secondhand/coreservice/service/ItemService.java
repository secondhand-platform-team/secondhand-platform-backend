package com.secondhand.coreservice.service;

import java.util.List;

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

    List<ItemResponse> getItemsByCategory(String categoryId);

    List<ItemResponse> getItemsByUser(String userId);

    ItemResponse updateItem(String itemId, ItemRequest request);

    MessageResponse deleteItem(String itemId);

    void handleVNPayCallback(VNPayCallbackRequest request);
}
