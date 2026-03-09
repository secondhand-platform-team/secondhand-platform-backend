package com.secondhand.authservice.controller;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(
                        @RequestBody LoginRequest request) {
                return ResponseEntity.ok(
                                authService.login(request));
        }

        @PostMapping("/register")
        public ResponseEntity<MessageResponse> register(
                        @Valid @RequestBody RegisterRequest request) {
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(authService.register(request));
        }

        @GetMapping("/me")
        public ResponseEntity<UserInfoResponse> getCurrentUser() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                UserInfoResponse userInfo = authService.getCurrentUser(email);
                return ResponseEntity.ok(userInfo);
        }

}
