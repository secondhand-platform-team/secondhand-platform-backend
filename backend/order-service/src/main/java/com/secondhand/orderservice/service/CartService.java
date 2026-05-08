package com.secondhand.orderservice.service;

import com.secondhand.orderservice.dto.request.CartItemRequest;
import com.secondhand.orderservice.model.Cart;

public interface CartService {
    Cart createOrGetCart(String userId);

    boolean createCartIfAbsent(String userId);

    Cart addItemToCart(String userId, CartItemRequest request);

    Cart updateItemQuantity(String userId, String itemId, Integer quantity);

    Cart removeItemFromCart(String userId, String itemId);
}