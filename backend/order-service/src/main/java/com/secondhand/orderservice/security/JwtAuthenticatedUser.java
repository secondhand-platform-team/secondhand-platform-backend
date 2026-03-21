package com.secondhand.orderservice.security;

public record JwtAuthenticatedUser(String userId, String email) {
}
