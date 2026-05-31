package com.secondhand.authservice.service.impl;

import com.secondhand.authservice.exception.BadRequestException;
import com.secondhand.authservice.model.User;
import com.secondhand.authservice.repository.UserRepository;
import com.secondhand.authservice.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    private static final String OTP_KEY_PREFIX = "password_reset_otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;

    @Override
    public void sendResetOtp(String email) {
        // Verify user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy tài khoản nào với email này."));

        // Check if user is a Google-only account (no password set)
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new BadRequestException(
                    "Tài khoản này đăng nhập bằng Google. Vui lòng sử dụng Google để đăng nhập.");
        }

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Store in Redis with 5-min TTL
        String key = OTP_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, OTP_TTL);

        // Send email
        sendOtpEmail(email, otp);

        log.info("Password reset OTP sent to {}", email);
    }

    @Override
    @Transactional
    public void verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        String key = OTP_KEY_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (!storedOtp.equals(otp)) {
            throw new BadRequestException("Mã OTP không đúng. Vui lòng kiểm tra lại.");
        }

        // Find user and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete OTP from Redis after successful reset
        redisTemplate.delete(key);

        log.info("Password reset successful for {}", email);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🔐 ReLife - Mã xác thực đặt lại mật khẩu");

            String htmlContent = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 520px; margin: 0 auto; background: #f8faf9; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.08);">
                  <div style="background: linear-gradient(135deg, #10b981 0%%, #0d9488 100%%); padding: 32px 24px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 800;">🌿 ReLife</h1>
                    <p style="color: rgba(255,255,255,0.9); margin: 8px 0 0; font-size: 14px;">Sàn mua bán đồ cũ bền vững</p>
                  </div>
                  <div style="padding: 32px 24px;">
                    <h2 style="color: #1e293b; margin: 0 0 12px; font-size: 20px;">Đặt lại mật khẩu</h2>
                    <p style="color: #64748b; margin: 0 0 24px; font-size: 15px; line-height: 1.6;">
                      Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng sử dụng mã OTP bên dưới:
                    </p>
                    <div style="background: white; border: 2px dashed #10b981; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 24px;">
                      <span style="font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #10b981;">%s</span>
                    </div>
                    <p style="color: #94a3b8; font-size: 13px; margin: 0; line-height: 1.6;">
                      ⏱ Mã có hiệu lực trong <strong>5 phút</strong>.<br>
                      Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                    </p>
                  </div>
                  <div style="padding: 16px 24px; background: #f1f5f9; text-align: center;">
                    <p style="color: #94a3b8; font-size: 12px; margin: 0;">© 2026 ReLife. Mọi quyền được bảo lưu.</p>
                  </div>
                </div>
                """.formatted(otp);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new BadRequestException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
}
