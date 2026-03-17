package com.secondhand.authservice.service;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.dto.response.UserProfileInfoResponse;
import com.secondhand.authservice.model.enums.Role;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse loginByRole(LoginRequest request, Role requiredRole);

    MessageResponse register(RegisterRequest request);

    MessageResponse registerUser(RegisterRequest request);

    MessageResponse registerAdmin(RegisterRequest request);

    UserInfoResponse getCurrentUser(String email);

    UserProfileInfoResponse getCurrentUserProfile(String email);
}
