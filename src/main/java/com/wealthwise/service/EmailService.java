package com.wealthwise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Skipping password reset email to {}", toEmail);
            return;
        }
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("WealthWise - Password Reset Request");
        message.setText(
            "Hello,\n\n"
            + "You requested a password reset for your WealthWise account.\n\n"
            + "Click the link below to reset your password:\n"
            + resetLink + "\n\n"
            + "This link will expire in 15 minutes.\n\n"
            + "If you did not request this, please ignore this email.\n\n"
            + "— WealthWise Team"
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
