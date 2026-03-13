package com.secondhand.orderservice.controller;

import com.secondhand.orderservice.dto.request.CreateCartRequest;
import com.secondhand.orderservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/carts")
@RequiredArgsConstructor
public class InternalCartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<Void> createCart(@Valid @RequestBody CreateCartRequest request) {
        boolean created = cartService.createCartIfAbsent(request.getUserId());
        if (created) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        return ResponseEntity.ok().build();
    }
}