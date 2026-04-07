package com.wealthwise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("WealthWise - Password Reset OTP");
        message.setText(
            "Hello,\n\n"
            + "Your password reset OTP is: " + otp + "\n\n"
            + "This OTP will expire in 15 minutes.\n\n"
            + "If you did not request this, please ignore this email.\n\n"
            + "— WealthWise Team"
        );

        try {
            mailSender.send(message);
            log.info("Password reset OTP sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send password reset OTP to {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }
}
