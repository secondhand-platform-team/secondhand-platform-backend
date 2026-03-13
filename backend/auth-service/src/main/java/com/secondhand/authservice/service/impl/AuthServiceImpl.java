package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.model.UserProfile;
import com.secondhand.authservice.model.enums.Role;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.service.AuthService;
import com.secondhand.authservice.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;

    @Value("${order.service.base-url}")
    private String orderServiceBaseUrl;

    @Override
    public AuthResponse login(LoginRequest request) {
        return loginByRole(request, Role.USER);
    }

    @Override
    public AuthResponse loginByRole(LoginRequest request, Role requiredRole) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getRole() != requiredRole) {
            throw new BadRequestException("Account does not have permission to login here");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtils.generateToken(userDetails);

        return new AuthResponse(token, "Bearer");
    }

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        return registerUser(request);
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
    public MessageResponse registerAdmin(RegisterRequest request) {
        registerWithRole(request, Role.ADMIN);
        return MessageResponse.success("Admin registration successful! Please login to continue.");
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
        String endpoint = orderServiceBaseUrl + "/api/internal/carts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity =
                new HttpEntity<>(Map.of("userId", userId), headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(endpoint, requestEntity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BadRequestException("Create cart failed at order-service");
            }
        } catch (RestClientException exception) {
            throw new BadRequestException("Cannot create cart for user", exception);
        }
    }

    @Override
    public UserInfoResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.isStatus());
    }
}
