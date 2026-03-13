package com.secondhand.orderservice.service;

public interface CartService {
    boolean createCartIfAbsent(String userId);
}