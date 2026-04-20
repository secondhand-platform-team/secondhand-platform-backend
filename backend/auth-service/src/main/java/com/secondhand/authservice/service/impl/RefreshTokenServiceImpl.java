package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.model.RefreshToken;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.repository.RefreshTokenRepository;
import com.secondhand.authservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Enforce single active session per user
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken validateToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenValue) {
        RefreshToken oldToken = validateToken(oldTokenValue);
        User user = oldToken.getUser();

        // Delete the old token
        refreshTokenRepository.delete(oldToken);

        // Issue a new one
        RefreshToken newToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    @Override
    @Transactional
    public void revokeByTokenValue(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    @Transactional
    public void revokeAllByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
