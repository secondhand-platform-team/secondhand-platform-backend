package com.secondhand.authservice.service;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.request.UpdateProfileRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.MessageResponse;
import com.secondhand.authservice.dto.response.UserInfoResponse;
import com.secondhand.authservice.dto.response.UserProfileInfoResponse;
import com.secondhand.authservice.model.enums.Role;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AuthService {

    AuthResponse loginByRole(LoginRequest request, Role requiredRole);
    
    AuthResponse loginWithGoogle(String idToken);

    MessageResponse registerUser(RegisterRequest request);

    MessageResponse registerStaffOrAdmin(RegisterRequest request, Role role);

    UserInfoResponse getCurrentUser(String identifier);

    UserProfileInfoResponse getCurrentUserProfile(String identifier);

    UserProfileInfoResponse getUserProfileByUserId(String userId);

    UserProfileInfoResponse updateProfile(String identifier, UpdateProfileRequest request);

    UserProfileInfoResponse updateAvatar(String identifier, MultipartFile file) throws IOException;
}
