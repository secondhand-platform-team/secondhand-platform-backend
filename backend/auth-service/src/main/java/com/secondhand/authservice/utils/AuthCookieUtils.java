package com.secondhand.authservice.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${app.security.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Access Token ──────────────────────────────────────────────────────────

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(accessExpirationMs / 1000)
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String extractTokenFromCookies(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
