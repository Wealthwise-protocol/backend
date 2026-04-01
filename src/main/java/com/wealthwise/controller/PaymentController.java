package com.wealthwise.controller;

import com.wealthwise.dto.request.CreatePaymentRequest;
import com.wealthwise.dto.request.ProcessPaymentRequest;
import com.wealthwise.dto.response.PaymentResponse;
import com.wealthwise.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a new payment
     */
    @PostMapping
    public ResponseEntity<Map<String, PaymentResponse>> createPayment(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        PaymentResponse response = paymentService.createPayment(userUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("payment", response));
    }

    /**
     * Get all payments for authenticated user
     */
    @GetMapping
    public ResponseEntity<Map<String, List<PaymentResponse>>> getUserPayments(
        @AuthenticationPrincipal String userId
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        List<PaymentResponse> payments = paymentService.getUserPayments(userUuid);
        return ResponseEntity.ok(Map.of("payments", payments));
    }

    /**
     * Get payments by status (SUCCESS, FAILED, PENDING)
     */
    @GetMapping("/by-status")
    public ResponseEntity<Map<String, List<PaymentResponse>>> getPaymentsByStatus(
        @AuthenticationPrincipal String userId,
        @RequestParam(required = false, defaultValue = "SUCCESS") String status
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        List<PaymentResponse> payments = paymentService.getUserPaymentsByStatus(userUuid, status);
        return ResponseEntity.ok(Map.of("payments", payments));
    }

    /**
     * Get single payment details
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, PaymentResponse>> getPayment(
        @AuthenticationPrincipal String userId,
        @PathVariable UUID paymentId
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        PaymentResponse payment = paymentService.getPayment(userUuid, paymentId);
        return ResponseEntity.ok(Map.of("payment", payment));
    }

    /**
     * Process payment for SIP creation
     * Validates payment and creates transaction record
     * Status: true = success, false = failure
     */
    @PostMapping("/process-sip-creation")
    public ResponseEntity<Map<String, PaymentResponse>> processPaymentForSipCreation(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody ProcessPaymentRequest request,
        @RequestParam(defaultValue = "true") boolean isSuccess,
        @RequestParam(required = false) String failureReason
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        PaymentResponse response = paymentService.processPaymentForSipCreation(
            userUuid,
            request.getPaymentId(),
            request.getSipId(),
            isSuccess,
            failureReason
        );
        return ResponseEntity.ok(Map.of("payment", response));
    }

    /**
     * Process payment for SIP installment
     * Validates payment and creates transaction record
     * Status: true = success, false = failure
     */
    @PostMapping("/process-installment")
    public ResponseEntity<Map<String, PaymentResponse>> processPaymentForInstallment(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody ProcessPaymentRequest request,
        @RequestParam(defaultValue = "true") boolean isSuccess,
        @RequestParam(required = false) String failureReason
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        PaymentResponse response = paymentService.processPaymentForInstallment(
            userUuid,
            request.getSipInstallmentId(),
            request.getPaymentId(),
            isSuccess,
            failureReason
        );
        return ResponseEntity.ok(Map.of("payment", response));
    }

    private UUID getAuthenticatedUserId(String userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
