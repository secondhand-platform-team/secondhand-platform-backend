package com.secondhand.authservice.controller;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.dto.response.UserProfileInfoResponse;
import com.secondhand.authservice.model.enums.Role;
import com.secondhand.authservice.service.AuthService;
import com.secondhand.authservice.utils.AuthCookieUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
        private final AuthCookieUtils authCookieUtils;

        @PostMapping("/login/user")
        public ResponseEntity<UserProfileInfoResponse> loginUser(
                        @RequestBody LoginRequest request) {
                String accessToken = authService.loginByRole(request, Role.USER).getAccessToken();
                UserProfileInfoResponse profile = authService.getCurrentUserProfile(request.getEmail());

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, authCookieUtils.createAccessTokenCookie(accessToken).toString())
                                .body(profile);
        }

        @PostMapping("/login/admin")
        public ResponseEntity<UserProfileInfoResponse> loginAdmin(
                        @RequestBody LoginRequest request) {
                String accessToken = authService.loginByRole(request, Role.ADMIN).getAccessToken();
                UserProfileInfoResponse profile = authService.getCurrentUserProfile(request.getEmail());

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, authCookieUtils.createAccessTokenCookie(accessToken).toString())
                                .body(profile);
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

        @GetMapping("/profile")
        public ResponseEntity<UserProfileInfoResponse> getCurrentUserProfile() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String email = authentication.getName();
                UserProfileInfoResponse profileInfo = authService.getCurrentUserProfile(email);
                return ResponseEntity.ok(profileInfo);
        }

        @GetMapping("/users/{userId}/profile")
        public ResponseEntity<UserProfileInfoResponse> getUserProfileByUserId(@PathVariable String userId) {
                UserProfileInfoResponse profileInfo = authService.getUserProfileByUserId(userId);
                return ResponseEntity.ok(profileInfo);
        }

        @PostMapping("/logout")
        public ResponseEntity<MessageResponse> logout() {
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, authCookieUtils.clearAccessTokenCookie().toString())
                                .body(MessageResponse.success("Đăng xuất thành công"));
        }

}
