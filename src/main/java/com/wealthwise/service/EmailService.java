package com.wealthwise.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;

    @Value("${MAIL_FROM:noreply@app.sathyam.xyz}")
    private String fromEmail;

    public EmailService(@Value("${RESEND_API_KEY}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("WealthWise - Password Reset OTP")
                .html(
                    "<p>Hello,</p>"
                    + "<p>Your password reset OTP is: <strong>" + otp + "</strong></p>"
                    + "<p>This OTP will expire in 10 minutes.</p>"
                    + "<p>If you did not request this, please ignore this email.</p>"
                    + "<p>— WealthWise Team</p>"
                )
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            log.info("Password reset OTP sent to {} (id: {})", toEmail, response.getId());
        } catch (Exception e) {
            log.error("Failed to send password reset OTP to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
