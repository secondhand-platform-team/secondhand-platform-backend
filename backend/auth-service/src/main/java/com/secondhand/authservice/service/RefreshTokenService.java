package com.secondhand.authservice.service;

import com.secondhand.authservice.model.RefreshToken;
import com.secondhand.authservice.model.User;

public interface RefreshTokenService {

    /**
     * Create a new refresh token for the given user.
     * Revokes any existing token for this user (single-session).
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Validate the token value. Throws BadRequestException if invalid or expired.
     */
    RefreshToken validateToken(String tokenValue);

    /**
     * Validate old token, delete it, and issue a brand-new one (rotation).
     */
    RefreshToken rotateRefreshToken(String oldTokenValue);

    /**
     * Revoke a specific refresh token by its value.
     */
    void revokeByTokenValue(String tokenValue);

    /**
     * Revoke all refresh tokens belonging to a user (use on logout-all-devices).
     */
    void revokeAllByUser(User user);
}
