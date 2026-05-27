package com.secondhand.authservice.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME_USER = "accessToken_user";
    public static final String REFRESH_TOKEN_COOKIE_NAME_USER = "refreshToken_user";
    public static final String ACCESS_TOKEN_COOKIE_NAME_ADMIN = "accessToken_admin";
    public static final String REFRESH_TOKEN_COOKIE_NAME_ADMIN = "refreshToken_admin";

    @Value("${app.security.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.security.cookie.same-site:None}")
    private String cookieSameSite;

    @Value("${jwt.expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Access Token ──────────────────────────────────────────────────────────

    public ResponseCookie createAccessTokenCookieForUser(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME_USER, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(accessExpirationMs / 1000)
                .build();
    }

    public ResponseCookie createAccessTokenCookieForAdmin(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME_ADMIN, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(accessExpirationMs / 1000)
                .build();
    }

    public ResponseCookie clearAccessTokenCookieForUser() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME_USER, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearAccessTokenCookieForAdmin() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME_ADMIN, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    public ResponseCookie createRefreshTokenCookieForUser(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME_USER, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .build();
    }

    public ResponseCookie createRefreshTokenCookieForAdmin(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME_ADMIN, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookieForUser() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME_USER, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie clearRefreshTokenCookieForAdmin() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME_ADMIN, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
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

    public String extractTokenFromCookies(HttpServletRequest request, String... cookieNames) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookieNames == null) return null;
        for (String cookieName : cookieNames) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
