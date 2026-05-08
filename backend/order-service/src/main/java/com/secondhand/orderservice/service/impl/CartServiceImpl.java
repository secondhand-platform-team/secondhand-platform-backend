package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.model.Cart;
import com.secondhand.orderservice.repository.CartRepository;
import com.secondhand.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.secondhand.orderservice.dto.request.CartItemRequest;
import com.secondhand.orderservice.model.CartItem;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    @Transactional
    public Cart createOrGetCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            cart = new Cart();
            cart.setId(UUID.randomUUID().toString());
            cart.setUserId(userId);
            cart.setCartItems(new ArrayList<>());
            return cartRepository.save(cart);
        }
        // Force initialization
        if (cart.getCartItems() != null) {
            cart.getCartItems().size();
        }
        return cart;
    }

    @Override
    @Transactional
    public boolean createCartIfAbsent(String userId) {
        if (cartRepository.existsByUserId(userId)) {
            return false;
        }

        Cart cart = new Cart();
        cart.setId(UUID.randomUUID().toString());
        cart.setUserId(userId);
        cart.setCartItems(new ArrayList<>());

        cartRepository.save(cart);
        return true;
    }

    @Override
    @Transactional
    public Cart addItemToCart(String userId, CartItemRequest request) {
        Cart cart = createOrGetCart(userId);
        
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getItemId().equals(request.getItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = new CartItem();
            newItem.setId(UUID.randomUUID().toString());
            newItem.setItemId(request.getItemId());
            newItem.setPrice(request.getPrice());
            newItem.setQuantity(request.getQuantity());
            newItem.setCart(cart);
            cart.getCartItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart updateItemQuantity(String userId, String itemId, Integer quantity) {
        Cart cart = createOrGetCart(userId);
        cart.getCartItems().stream()
                .filter(item -> item.getItemId().equals(itemId))
                .findFirst()
                .ifPresent(item -> {
                    if (quantity <= 0) {
                        cart.getCartItems().remove(item);
                    } else {
                        item.setQuantity(quantity);
                    }
                });
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart removeItemFromCart(String userId, String itemId) {
        Cart cart = createOrGetCart(userId);
        cart.getCartItems().removeIf(item -> item.getItemId().equals(itemId));
        return cartRepository.save(cart);
    }
}