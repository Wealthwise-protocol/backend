package com.wealthwise.service;

import com.wealthwise.dto.request.ChangePasswordRequest;
import com.wealthwise.dto.request.ForgotPasswordRequest;
import com.wealthwise.dto.request.ResetPasswordRequest;
import com.wealthwise.dto.request.SignInRequest;
import com.wealthwise.dto.request.SignUpRequest;
import com.wealthwise.dto.request.UpdateProfileRequest;
import com.wealthwise.dto.response.UserResponse;
import com.wealthwise.entity.PasswordResetToken;
import com.wealthwise.entity.User;
import com.wealthwise.repository.PasswordResetTokenRepository;
import com.wealthwise.repository.UserRepository;
import com.wealthwise.security.JwtService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long RESET_TOKEN_EXPIRY_MINUTES = 15L;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Transactional
    public Map<String, Object> signup(SignUpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
        }

        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(normalizedEmail)
            .phone(request.getPhone())
            .countryCode(defaultCountryCode(request.getCountryCode()))
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .kycVerified(false)
            .role("USER")
            .enabled(true)
            .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId());

        return Map.of(
            "user", toUserResponse(savedUser),
            "token", token
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> signin(SignInRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId());

        return Map.of(
            "user", toUserResponse(user),
            "token", token
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = getUserOrThrow(userId);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);

        if (userRepository.existsByPhoneAndIdNot(request.getPhone(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        return toUserResponse(updatedUser);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(normalizeEmail(request.getEmail())).ifPresent(user -> {
            String otp = generateOtp();
            
            PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();

            passwordResetTokenRepository.save(token);
            emailService.sendPasswordResetOtp(user.getEmail(), otp);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getOtp())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        // With @OnDelete(CASCADE) annotations, related entities will be automatically deleted
        // But we still explicitly delete tokens to ensure cleanup
        User user = getUserOrThrow(userId);
        passwordResetTokenRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String defaultCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "+91";
        }
        return countryCode;
    }

    private String generateOtp() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .countryCode(user.getCountryCode())
            .kycVerified(user.getKycVerified())
            .build();
    }
}

