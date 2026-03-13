package com.secondhand.orderservice.service.impl;

import com.secondhand.orderservice.model.Cart;
import com.secondhand.orderservice.repository.CartRepository;
import com.secondhand.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

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
}