package com.secondhand.authservice.controller;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.model.enums.Role;
import com.secondhand.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @PostMapping("/login/user")
        public ResponseEntity<AuthResponse> loginUser(
                        @RequestBody LoginRequest request) {
                return ResponseEntity.ok(
                                authService.loginByRole(request, Role.USER));
        }

        @PostMapping("/login/admin")
        public ResponseEntity<AuthResponse> loginAdmin(
                        @RequestBody LoginRequest request) {
                return ResponseEntity.ok(
                                authService.loginByRole(request, Role.ADMIN));
        }


        @PostMapping("/register/user")
        public ResponseEntity<MessageResponse> registerUser(
                        @Valid @RequestBody RegisterRequest request) {
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(authService.registerUser(request));
        }

        @PostMapping("/register/admin")
        public ResponseEntity<MessageResponse> registerAdmin(
                        @Valid @RequestBody RegisterRequest request) {
                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(authService.registerAdmin(request));
        }

        @GetMapping("/me")
        public ResponseEntity<UserInfoResponse> getCurrentUser() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                UserInfoResponse userInfo = authService.getCurrentUser(email);
                return ResponseEntity.ok(userInfo);
        }

}
