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

                String token = extractAccessTokenFromCookies(request.getCookies());

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

        private String extractAccessTokenFromCookies(Cookie[] cookies) {
                if (cookies == null) {
                        return null;
                }

                for (Cookie cookie : cookies) {
                        if (AuthCookieUtils.ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                                return cookie.getValue();
                        }
                }

                return null;
        }
}
