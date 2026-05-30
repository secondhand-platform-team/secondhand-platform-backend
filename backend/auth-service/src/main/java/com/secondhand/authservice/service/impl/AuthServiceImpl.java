package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.request.UpdateProfileRequest;
import com.secondhand.authservice.dto.request.ChangePasswordRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.dto.response.UserProfileInfoResponse;
import com.secondhand.authservice.dto.response.UserProfileResponse;
import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.client.CartRestClient;
import com.secondhand.authservice.model.RefreshToken;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.model.UserProfile;
import com.secondhand.authservice.model.enums.Role;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.service.AuthService;
import com.secondhand.authservice.service.CloudinaryService;
import com.secondhand.authservice.service.RefreshTokenService;
import com.secondhand.authservice.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CartRestClient cartRestClient;
    private final CloudinaryService cloudinaryService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.google.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse loginByRole(LoginRequest request, Role requiredRole) {

        User user = userRepository.findByEmailOrPhoneNumber(request.getEmail(), request.getEmail())
                .orElseThrow(() -> new BadRequestException(
                        "Không có tài khoản nào được đăng ký bằng email hoặc số điện thoại này trong hệ thống."));

        if (requiredRole == Role.USER && user.getRole() != Role.USER) {
            throw new BadRequestException("Tài khoản quản trị viên không thể đăng nhập vào trang người dùng.");
        }

        if ((requiredRole == Role.ADMIN || requiredRole == Role.STAFF) && user.getRole() == Role.USER) {
            throw new BadRequestException("Tài khoản người dùng không có quyền truy cập vào trang quản trị.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtUtils.generateToken(userDetails, user.getUserId());

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return new AuthResponse(accessToken, refreshToken.getToken(), "Bearer");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new BadRequestException("Sai mật khẩu, vui lòng kiểm tra lại.");
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new BadRequestException("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BadRequestException("ID Token Google không hợp lệ hoặc đã hết hạn.");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            String fullName = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> registerNewGoogleUser(email, fullName, pictureUrl));

            if (user.getRole() != Role.USER) {
                throw new BadRequestException("Tài khoản quản trị viên không được phép đăng nhập qua Google.");
            }

            if (!user.isStatus()) {
                throw new BadRequestException("Tài khoản của bạn đã bị khóa.");
            }

            // Create UserDetails for JWT generation
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    "", // No password for OAuth users
                    user.isStatus(),
                    true, true, true,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );

            String accessToken = jwtUtils.generateToken(userDetails, user.getUserId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            return new AuthResponse(accessToken, refreshToken.getToken(), "Bearer");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Đăng nhập bằng Google thất bại: " + e.getMessage());
        }
    }

    private User registerNewGoogleUser(String email, String fullName, String pictureUrl) {
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(email)
                .password("") // Google users don't have a password
                .role(Role.USER)
                .status(true)
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .freeSellUsed(2)
                .build();

        UserProfile userProfile = UserProfile.builder()
                .user(user)
                .fullName(fullName)
                .avatarUrl(pictureUrl)
                .build();

        user.setUserProfile(userProfile);
        user = userRepository.save(user);
        
        // Create cart for the new user
        try {
            createCartForUser(user.getUserId());
        } catch (Exception e) {
            // Log error but don't fail login if cart creation fails
            // log.error("Failed to create cart for Google user {}: {}", email, e.getMessage());
        }
        
        return user;
    }

    @Override
    @Transactional
    public MessageResponse registerUser(RegisterRequest request) {
        User user = registerWithRole(request, Role.USER);
        createCartForUser(user.getUserId());
        return MessageResponse.success("User registration successful! Please login to continue.");
    }

    @Override
    @Transactional
    public MessageResponse registerStaffOrAdmin(RegisterRequest request, Role role) {
        if (role != Role.STAFF && role != Role.ADMIN) {
            throw new BadRequestException("Invalid role for staff/admin registration");
        }
        registerWithRole(request, role);
        return MessageResponse.success(role.name() + " registration successful! Please login to continue.");
    }

    private User registerWithRole(RegisterRequest request, Role role) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        // Create new user
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(true)
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .freeSellUsed(2)
                .build();

        // Create user profile
        UserProfile userProfile = UserProfile.builder()
                .user(user)
                .fullName(request.getFullName())
                .build();

        user.setUserProfile(userProfile);

        // Save user (cascades to user profile)
        return userRepository.save(user);
    }

    private void createCartForUser(String userId) {
        cartRestClient.createCart(userId);
    }

    @Override
    public UserInfoResponse getCurrentUser(String identifier) {
        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isStatus(), user.getFreeSellUsed(),
                user.getCreatedAt());

    }

    @Override
    public UserProfileInfoResponse getCurrentUserProfile(String identifier) {
        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserInfoResponse userInfo = new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isStatus(),
                user.getFreeSellUsed(),
                user.getCreatedAt());

        UserProfile userProfile = user.getUserProfile();
        UserProfileResponse profileResponse = null;
        if (userProfile != null) {
            profileResponse = new UserProfileResponse(
                    userProfile.getFullName(),
                    userProfile.getAvatarUrl(),
                    userProfile.getDateOfBirth(),
                    userProfile.getGender(),
                    userProfile.getBio());
        }

        return new UserProfileInfoResponse(userInfo, profileResponse);
    }

    @Override
    public UserProfileInfoResponse getUserProfileByUserId(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserInfoResponse userInfo = new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isStatus(), user.getFreeSellUsed(),
                user.getCreatedAt());
        UserProfile userProfile = user.getUserProfile();
        UserProfileResponse profileResponse = null;
        if (userProfile != null) {
            profileResponse = new UserProfileResponse(
                    userProfile.getFullName(),
                    userProfile.getAvatarUrl(),
                    userProfile.getDateOfBirth(),
                    userProfile.getGender(),
                    userProfile.getBio());
        }

        return new UserProfileInfoResponse(userInfo, profileResponse);
    }

    @Override
    @Transactional
    public UserProfileInfoResponse updateProfile(String identifier, UpdateProfileRequest request) {
        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Update phone number on User entity
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // Update or create UserProfile
        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
        }

        if (request.getFullName() != null)
            userProfile.setFullName(request.getFullName());
        if (request.getBio() != null)
            userProfile.setBio(request.getBio());
        if (request.getGender() != null)
            userProfile.setGender(request.getGender());
        if (request.getDateOfBirth() != null)
            userProfile.setDateOfBirth(request.getDateOfBirth());

        userRepository.save(user);
        return getCurrentUserProfile(user.getEmail());
    }

    @Override
    @Transactional
    public UserProfileInfoResponse updateAvatar(String identifier, MultipartFile file) throws IOException {
        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String avatarUrl = cloudinaryService.uploadImage(file);

        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
        }
        userProfile.setAvatarUrl(avatarUrl);

        userRepository.save(user);
        return getCurrentUserProfile(user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(String identifier, ChangePasswordRequest request) {
        User user = userRepository.findByEmailOrPhoneNumber(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
