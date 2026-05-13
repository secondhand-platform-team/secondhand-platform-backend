package com.secondhand.authservice.controller;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.request.UpdateProfileRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.LoginResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.dto.response.UserProfileInfoResponse;
import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.model.RefreshToken;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.model.enums.Role;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.security.AuthUserDetailsService;
import com.secondhand.authservice.service.AuthService;
import com.secondhand.authservice.service.RefreshTokenService;
import com.secondhand.authservice.service.UserService;
import com.secondhand.authservice.utils.AuthCookieUtils;
import com.secondhand.authservice.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AuthCookieUtils authCookieUtils;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;
    private final AuthUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login/user")
    public ResponseEntity<LoginResponse> loginClient(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return doLogin(request, Role.USER, response);
    }

    @PostMapping("/login/admin")
    public ResponseEntity<LoginResponse> loginAdmin(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {
        // Admin portal accepts both STAFF and ADMIN roles
        User user = userRepository.findByEmailOrPhoneNumber(request.getEmail(), request.getEmail())
                .orElseThrow(() -> new BadRequestException(
                        "Không có tài khoản nào được đăng ký bằng email hoặc số điện thoại này trong hệ thống."));

        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only STAFF and ADMIN can access admin portal");
        }

        return doLogin(request, user.getRole(), response);
    }

    private ResponseEntity<LoginResponse> doLogin(
            LoginRequest request, Role role, HttpServletResponse response) {
        AuthResponse authResponse = authService.loginByRole(request, role);
        UserProfileInfoResponse profile = authService.getCurrentUserProfile(request.getEmail());

        // Set tokens as HttpOnly cookies — never exposed to JavaScript
        response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createAccessTokenCookie(authResponse.getAccessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createRefreshTokenCookie(authResponse.getRefreshToken()).toString());

        // Return only user info — no tokens in response body
        return ResponseEntity.ok(new LoginResponse(profile.getUser(), profile.getUserProfile()));
    }

    // ── Token Refresh ─────────────────────────────────────────────────────────

    /**
     * Reads the refreshToken HttpOnly cookie, validates it, rotates it,
     * and sets fresh accessToken + refreshToken cookies.
     * The old refresh token is deleted from the database (rotation).
     */
    @PostMapping("/refresh")
    public ResponseEntity<MessageResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String oldRefreshValue = authCookieUtils.extractTokenFromCookies(
                request, AuthCookieUtils.REFRESH_TOKEN_COOKIE_NAME);

        if (oldRefreshValue == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error("Refresh token không tìm thấy. Vui lòng đăng nhập lại."));
        }

        // Rotate: validates old token, deletes it, creates new one
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(oldRefreshValue);
        String userEmail = newRefreshToken.getUser().getEmail();
        String userId = newRefreshToken.getUser().getUserId();

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        String newAccessToken = jwtUtils.generateToken(userDetails, userId);

        response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createAccessTokenCookie(newAccessToken).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createRefreshTokenCookie(newRefreshToken.getToken()).toString());

        return ResponseEntity.ok(MessageResponse.success("Token đã được làm mới thành công"));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshTokenValue = authCookieUtils.extractTokenFromCookies(
                request, AuthCookieUtils.REFRESH_TOKEN_COOKIE_NAME);

        // Invalidate refresh token in DB if present
        if (refreshTokenValue != null) {
            refreshTokenService.revokeByTokenValue(refreshTokenValue);
        }

        // Clear both cookies regardless
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearRefreshTokenCookie().toString());

        return ResponseEntity.ok(MessageResponse.success("Đăng xuất thành công"));
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register/user")
    public ResponseEntity<MessageResponse> registerUser(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(request));
    }

    @PostMapping("/register/admin")
    public ResponseEntity<MessageResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestParam String role) {
        Role roleEnum = Role.valueOf(role.toUpperCase());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStaffOrAdmin(request, roleEnum));
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(authService.getCurrentUser(email));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileInfoResponse> getCurrentUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(authService.getCurrentUserProfile(email));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<UserProfileInfoResponse> getUserProfileByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserProfileByUserId(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileInfoResponse> updateProfile(
            @RequestBody UpdateProfileRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(authService.updateProfile(email, request));
    }

    @PutMapping("/profile/avatar")
    public ResponseEntity<?> updateAvatar(
            @RequestParam("file") MultipartFile file) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return ResponseEntity.ok(authService.updateAvatar(email, file));
        } catch (Exception e) {
            log.error("Avatar upload failed for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getClass().getSimpleName(), "message",
                            e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // ── Free-sell Usage ───────────────────────────────────────────────────────

    @PutMapping("/users/{userId}/free-sell-use/decrease")
    public ResponseEntity<Void> decreaseFreeSellUse(@PathVariable String userId) {
        userService.decreaseFreeSellUse(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}/free-sell-use")
    public ResponseEntity<Integer> getFreeSellUsed(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getFreeSellUsed(userId));
    }
}
