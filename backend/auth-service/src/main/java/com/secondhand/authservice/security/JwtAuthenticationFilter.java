package com.secondhand.authservice.security;

import com.secondhand.authservice.utils.AuthCookieUtils;
import com.secondhand.authservice.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private static final String BEARER_PREFIX = "Bearer ";

        private final JwtUtils jwtUtils;
        private final AuthUserDetailsService userDetailsService;

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getServletPath();
                // Skip filter for public auth endpoints
                return path.startsWith("/api/auth/login")
                                || path.startsWith("/api/auth/register")
                                || path.startsWith("/api/login")
                                || path.startsWith("/api/register");
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain) throws ServletException, IOException {

                String token = extractToken(request);

                if (token != null && jwtUtils.validateToken(token)) {
                        String email = jwtUtils.extractEmail(token);

                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities());

                        authentication.setDetails(
                                        new WebAuthenticationDetailsSource()
                                                        .buildDetails(request));

                        SecurityContextHolder.getContext()
                                        .setAuthentication(authentication);
                }

                filterChain.doFilter(request, response);
        }

        private String extractToken(HttpServletRequest request) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                        return authHeader.substring(BEARER_PREFIX.length());
                }

                return extractAccessTokenFromCookies(request);
        }

        private String extractAccessTokenFromCookies(HttpServletRequest request) {
                String clientType = request.getHeader("X-Client-Type");
                if (clientType != null && !clientType.isBlank()) {
                        if ("admin".equalsIgnoreCase(clientType)) {
                                return extractCookieValue(request.getCookies(), AuthCookieUtils.ACCESS_TOKEN_COOKIE_NAME_ADMIN);
                        }
                        if ("user".equalsIgnoreCase(clientType)) {
                                return extractCookieValue(request.getCookies(), AuthCookieUtils.ACCESS_TOKEN_COOKIE_NAME_USER);
                        }
                }

                String path = request.getServletPath();
                if (path != null && path.startsWith("/api/admin")) {
                        return extractCookieValue(request.getCookies(), AuthCookieUtils.ACCESS_TOKEN_COOKIE_NAME_ADMIN);
                }

                return extractCookieValue(request.getCookies(), AuthCookieUtils.ACCESS_TOKEN_COOKIE_NAME_USER);
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
