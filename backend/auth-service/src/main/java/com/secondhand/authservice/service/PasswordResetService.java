package com.secondhand.authservice.service;

public interface PasswordResetService {

    /**
     * Generates a 6-digit OTP, stores it in Redis (5 min TTL),
     * and sends it to the user's email.
     */
    void sendResetOtp(String email);

    /**
     * Verifies the OTP from Redis and resets the user's password.
     */
    void verifyOtpAndResetPassword(String email, String otp, String newPassword);
}
