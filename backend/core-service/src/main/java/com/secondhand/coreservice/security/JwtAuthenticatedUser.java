package com.secondhand.coreservice.security;

public record JwtAuthenticatedUser(String userId, String email) {
}
