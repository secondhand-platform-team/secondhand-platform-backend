package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.request.CartItemRequest;
import com.secondhand.orderservice.model.Cart;
import com.secondhand.orderservice.security.JwtAuthenticatedUser;
import com.secondhand.orderservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/me")
    public ResponseEntity<Cart> getMyCart(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        return ResponseEntity.ok(cartService.createOrGetCart(user.userId()));
    }

    @PostMapping("/me/items")
    public ResponseEntity<Cart> addItemToCart(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItemToCart(user.userId(), request));
    }

    @PutMapping("/me/items/{itemId}")
    public ResponseEntity<Cart> updateItemQuantity(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String itemId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateItemQuantity(user.userId(), itemId, quantity));
    }

    @DeleteMapping("/me/items/{itemId}")
    public ResponseEntity<Cart> removeItemFromCart(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable String itemId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(user.userId(), itemId));
    }
}
