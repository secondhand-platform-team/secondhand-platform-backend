package com.secondhand.orderservice.service;

import com.secondhand.orderservice.model.Cart;

public interface CartService {
    Cart createOrGetCart(String userId);

    boolean createCartIfAbsent(String userId);

    Cart addItemToCart(String userId, String itemId);

    Cart removeItemFromCart(String userId, String itemId);
}