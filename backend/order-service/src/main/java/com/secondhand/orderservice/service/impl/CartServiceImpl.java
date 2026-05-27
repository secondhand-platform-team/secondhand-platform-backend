package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.model.Cart;
import com.secondhand.orderservice.model.CartItem;
import com.secondhand.orderservice.repository.CartRepository;
import com.secondhand.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public Cart addItemToCart(String userId, String itemId) {
        Cart cart = createOrGetCart(userId);

        // Kiểm tra item đã tồn tại trong cart chưa (secondhand: mỗi item là duy nhất)
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getItemId().equals(itemId))
                .findFirst();

        if (existingItem.isPresent()) {
            // Item đã có trong cart → không thêm nữa
            return cart;
        }

        CartItem newItem = new CartItem();
        newItem.setId(UUID.randomUUID().toString());
        newItem.setItemId(itemId);
        newItem.setCart(cart);
        newItem.setCreatedAt(LocalDateTime.now());
        cart.getCartItems().add(newItem);

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