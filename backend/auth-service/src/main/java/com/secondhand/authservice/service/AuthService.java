package com.secondhand.authservice.service;

import com.secondhand.authservice.dto.request.LoginRequest;
import com.secondhand.authservice.dto.request.RegisterRequest;
import com.secondhand.authservice.dto.response.AuthResponse;
import com.secondhand.authservice.dto.response.MessageResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    MessageResponse register(RegisterRequest request);
}
