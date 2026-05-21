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
        return doLogin(request, Role.ADMIN, response);
    }

    @PostMapping("/login/google")
    public ResponseEntity<LoginResponse> loginGoogle(
            @RequestBody @Valid com.secondhand.authservice.dto.request.GoogleLoginRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.loginWithGoogle(request.getIdToken());
        UserProfileInfoResponse profile = authService.getCurrentUserProfile(payloadEmail(request.getIdToken()));

        // Set tokens as HttpOnly cookies
        response.addHeader(HttpHeaders.SET_COOKIE,
            authCookieUtils.createAccessTokenCookieForUser(authResponse.getAccessToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
            authCookieUtils.createRefreshTokenCookieForUser(authResponse.getRefreshToken()).toString());

        return ResponseEntity.ok(new LoginResponse(profile.getUser(), profile.getUserProfile()));
    }

    private String payloadEmail(String idToken) {
        // Simple extraction for the response, the actual verification happens in service
        try {
            return com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.parse(
                    new com.google.api.client.json.gson.GsonFactory(), idToken).getPayload().getEmail();
        } catch (Exception e) {
            throw new BadRequestException("ID Token không hợp lệ.");
        }
    }

    private ResponseEntity<LoginResponse> doLogin(
            LoginRequest request, Role role, HttpServletResponse response) {
        AuthResponse authResponse = authService.loginByRole(request, role);
        UserProfileInfoResponse profile = authService.getCurrentUserProfile(request.getEmail());

        // Set tokens as HttpOnly cookies — never exposed to JavaScript
        if (role == Role.USER) {
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createAccessTokenCookieForUser(authResponse.getAccessToken()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createRefreshTokenCookieForUser(authResponse.getRefreshToken()).toString());
        } else {
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createAccessTokenCookieForAdmin(authResponse.getAccessToken()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createRefreshTokenCookieForAdmin(authResponse.getRefreshToken()).toString());
        }

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
        String clientType = request.getHeader("X-Client-Type");
        if (clientType == null || clientType.isBlank()) {
            clientType = request.getParameter("client");
        }

        String adminRefresh = authCookieUtils.extractTokenFromCookies(
                request, AuthCookieUtils.REFRESH_TOKEN_COOKIE_NAME_ADMIN);
        String userRefresh = authCookieUtils.extractTokenFromCookies(
                request, AuthCookieUtils.REFRESH_TOKEN_COOKIE_NAME_USER);

        String oldRefreshValue = null;
        boolean isAdminClient = false;

        if ("admin".equalsIgnoreCase(clientType)) {
            oldRefreshValue = adminRefresh;
            isAdminClient = true;
        } else if ("user".equalsIgnoreCase(clientType)) {
            oldRefreshValue = userRefresh;
            isAdminClient = false;
        } else if (adminRefresh != null && userRefresh != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.error("Please specify client type via X-Client-Type or ?client=admin|user"));
        } else if (adminRefresh != null) {
            oldRefreshValue = adminRefresh;
            isAdminClient = true;
        } else {
            oldRefreshValue = userRefresh;
            isAdminClient = false;
        }

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

        if (isAdminClient) {
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createAccessTokenCookieForAdmin(newAccessToken).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createRefreshTokenCookieForAdmin(newRefreshToken.getToken()).toString());
        } else {
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createAccessTokenCookieForUser(newAccessToken).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                authCookieUtils.createRefreshTokenCookieForUser(newRefreshToken.getToken()).toString());
        }

        return ResponseEntity.ok(MessageResponse.success("Token đã được làm mới thành công"));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String clientType = request.getHeader("X-Client-Type");
        if (clientType == null || clientType.isBlank()) {
            clientType = request.getParameter("client");
        }

        String adminRefresh = authCookieUtils.extractTokenFromCookies(
                request, AuthCookieUtils.REFRESH_TOKEN_COOKIE_NAME_ADMIN);
        String userRefresh = authCookieUtils.extractTokenFromCookies(
                request, AuthCookieUtils.REFRESH_TOKEN_COOKIE_NAME_USER);

        if ("admin".equalsIgnoreCase(clientType)) {
            if (adminRefresh != null) {
                refreshTokenService.revokeByTokenValue(adminRefresh);
            }
            response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearAccessTokenCookieForAdmin().toString());
            response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearRefreshTokenCookieForAdmin().toString());
            return ResponseEntity.ok(MessageResponse.success("Đăng xuất thành công"));
        }

        if ("user".equalsIgnoreCase(clientType)) {
            if (userRefresh != null) {
                refreshTokenService.revokeByTokenValue(userRefresh);
            }
            response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearAccessTokenCookieForUser().toString());
            response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearRefreshTokenCookieForUser().toString());
            return ResponseEntity.ok(MessageResponse.success("Đăng xuất thành công"));
        }

        if (adminRefresh != null) {
            refreshTokenService.revokeByTokenValue(adminRefresh);
        }
        if (userRefresh != null) {
            refreshTokenService.revokeByTokenValue(userRefresh);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearAccessTokenCookieForAdmin().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearRefreshTokenCookieForAdmin().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearAccessTokenCookieForUser().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieUtils.clearRefreshTokenCookieForUser().toString());

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
