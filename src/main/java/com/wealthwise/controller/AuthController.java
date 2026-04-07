package com.wealthwise.controller;

import com.wealthwise.dto.request.ChangePasswordRequest;
import com.wealthwise.dto.request.ForgotPasswordRequest;
import com.wealthwise.dto.request.ResetPasswordRequest;
import com.wealthwise.dto.request.SignInRequest;
import com.wealthwise.dto.request.SignUpRequest;
import com.wealthwise.dto.request.UpdateProfileRequest;
import com.wealthwise.dto.response.UserResponse;
import com.wealthwise.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<Map<String, Object>> signin(@Valid @RequestBody SignInRequest request) {
        return ResponseEntity.ok(authService.signin(request));
    }

    @PostMapping("/signout")
    public ResponseEntity<Map<String, Boolean>> signout() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, UserResponse>> me(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(Map.of("user", authService.getCurrentUser(userId)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<Map<String, UserResponse>> updateProfile(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        UserResponse user = authService.updateProfile(userId, request);
        return ResponseEntity.ok(Map.of("user", user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Boolean>> changePassword(
        Authentication authentication,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, Boolean>> deleteAccount(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        authService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}

