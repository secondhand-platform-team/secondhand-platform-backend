package com.secondhand.chatservice.security;

public record JwtAuthenticatedUser(String userId, String email) {
}
