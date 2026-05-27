package com.secondhand.orderservice.security;

import com.secondhand.orderservice.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_COOKIE_NAME_USER = "accessToken_user";
    private static final String ACCESS_TOKEN_COOKIE_NAME_ADMIN = "accessToken_admin";

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Try Authorization header first, then cookies
        String token = extractAccessTokenFromHeader(request);
        System.out.println("in token"+ token);
        if (token == null) {
            token = extractAccessTokenFromCookies(request);
        }

        if (token != null && jwtUtils.validateToken(token)) {
            JwtAuthenticatedUser principal = new JwtAuthenticatedUser(
                    jwtUtils.extractUserId(token),
                    jwtUtils.extractEmail(token));

            List<SimpleGrantedAuthority> authorities = jwtUtils.extractRoles(token)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractAccessTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        System.out.println("auth header "+authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String extractAccessTokenFromCookies(HttpServletRequest request) {
        String clientType = request.getHeader("X-Client-Type");
        if (clientType != null && !clientType.isBlank()) {
            if ("admin".equalsIgnoreCase(clientType)) {
                return extractCookieValue(request.getCookies(), ACCESS_TOKEN_COOKIE_NAME_ADMIN);
            }
            if ("user".equalsIgnoreCase(clientType)) {
                return extractCookieValue(request.getCookies(), ACCESS_TOKEN_COOKIE_NAME_USER);
            }
        }

        String path = request.getServletPath();
        if (path != null && path.startsWith("/api/admin")) {
            return extractCookieValue(request.getCookies(), ACCESS_TOKEN_COOKIE_NAME_ADMIN);
        }

        return extractCookieValue(request.getCookies(), ACCESS_TOKEN_COOKIE_NAME_USER);
    }

    private String extractCookieValue(Cookie[] cookies, String cookieName) {
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
