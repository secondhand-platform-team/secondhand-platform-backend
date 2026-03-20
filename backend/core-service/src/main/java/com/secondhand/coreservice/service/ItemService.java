package com.secondhand.coreservice.service;

import java.util.List;

import com.secondhand.coreservice.dto.request.ItemRequest;
import com.secondhand.coreservice.dto.response.ItemResponse;
import com.secondhand.coreservice.dto.response.MessageResponse;

public interface ItemService {
    ItemResponse createItem(ItemRequest request);
    ItemResponse getItemById(String itemId);
    List<ItemResponse> getAllItems();
    List<ItemResponse> getItemsByCategory(String categoryId);
    List<ItemResponse> getItemsByUser(String userId);
    ItemResponse updateItem(String itemId, ItemRequest request);
    MessageResponse deleteItem(String itemId);
}
