package com.wealthwise.service;

import com.wealthwise.dto.request.CreatePaymentRequest;
import com.wealthwise.dto.response.PaymentResponse;
import com.wealthwise.entity.Payment;
import com.wealthwise.entity.Sip;
import com.wealthwise.entity.SipInstallment;
import com.wealthwise.entity.Transaction;
import com.wealthwise.entity.User;
import com.wealthwise.repository.PaymentRepository;
import com.wealthwise.repository.SipInstallmentRepository;
import com.wealthwise.repository.SipRepository;
import com.wealthwise.repository.TransactionRepository;
import com.wealthwise.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * PaymentService handles payment processing for SIP creation and installments.
 * - Creates payment records
 * - Processes payments for SIP creation
 * - Processes payments for SIP installments
 * - Handles failed payments and records transaction failures
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SipRepository sipRepository;
    private final SipInstallmentRepository sipInstallmentRepository;
    private final TransactionRepository transactionRepository;
    private final MfApiService mfApiService;

    /**
     * Create a new payment record
     */
    @Transactional
    public PaymentResponse createPayment(UUID userId, CreatePaymentRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Payment payment = Payment.builder()
            .user(user)
            .amount(request.getAmount())
            .paymentMethod(request.getPaymentMethod().toUpperCase())
            .status("PENDING")
            .description(request.getDescription())
            .externalPaymentId(request.getExternalPaymentId())
            .createdAt(LocalDateTime.now())
            .build();

        Payment saved = paymentRepository.save(payment);
        return toPaymentResponse(saved);
    }

    /**
     * Process payment for SIP creation.
     * Creates transaction record only if payment is successful.
     * If payment fails, creates a FAILED transaction record.
     */
    @Transactional
    public PaymentResponse processPaymentForSipCreation(
        UUID userId,
        UUID paymentId,
        UUID sipId,
        boolean isSuccess,
        String failureReason
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        Sip sip = sipRepository.findByIdAndUserId(sipId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIP not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        if (isSuccess) {
            // Process successful payment
            payment.setStatus("SUCCESS");
            payment.setProcessedAt(now);
            paymentRepository.save(payment);

            // Create completed transaction
            BigDecimal nav = sip.getFund() != null ? sip.getFund().getNav() : BigDecimal.ZERO;
            BigDecimal units = payment.getAmount().divide(nav, 2, java.math.RoundingMode.HALF_UP);

            Transaction transaction = Transaction.builder()
                .user(user)
                .fundName(sip.getFundName())
                .date(today)
                .amount(payment.getAmount())
                .units(units)
                .nav(nav)
                .type("BUY")
                .status("COMPLETED")
                .build();

            transactionRepository.save(transaction);

            // Update SIP totals
            sip.setTotalInvested(sip.getTotalInvested().add(payment.getAmount()));
            sipRepository.save(sip);

        } else {
            // Process failed payment
            payment.setStatus("FAILED");
            payment.setFailureReason(failureReason);
            payment.setProcessedAt(now);
            paymentRepository.save(payment);

            // Create failed transaction record
            Transaction transaction = Transaction.builder()
                .user(user)
                .fundName(sip.getFundName())
                .date(today)
                .amount(payment.getAmount())
                .units(BigDecimal.ZERO)
                .nav(BigDecimal.ZERO)
                .type("BUY")
                .status("FAILED")
                .build();

            transactionRepository.save(transaction);
        }

        return toPaymentResponse(payment);
    }

    /**
     * Process payment for SIP installment.
     * Creates transaction record only if payment is successful.
     * If payment ID is not provided or payment fails, creates a FAILED transaction record.
     */
    @Transactional
    public PaymentResponse processPaymentForInstallment(
        UUID userId,
        UUID sipInstallmentId,
        UUID paymentId,
        boolean isSuccess,
        String failureReason
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        SipInstallment installment = sipInstallmentRepository.findById(sipInstallmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Installment not found"));

        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        Sip sip = installment.getSip();
        LocalDateTime now = LocalDateTime.now();

        if (isSuccess) {
            // Process successful payment
            payment.setStatus("SUCCESS");
            payment.setProcessedAt(now);
            paymentRepository.save(payment);

            // Update installment status
            installment.setStatus("COMPLETED");
            sipInstallmentRepository.save(installment);

            // Create completed transaction
            BigDecimal nav = sip.getFund() != null ? sip.getFund().getNav() : BigDecimal.ZERO;
            BigDecimal units = installment.getAmount().divide(nav, 2, java.math.RoundingMode.HALF_UP);

            Transaction transaction = Transaction.builder()
                .user(user)
                .fundName(sip.getFundName())
                .date(installment.getInstallmentDate())
                .amount(installment.getAmount())
                .units(units)
                .nav(nav)
                .type("BUY")
                .status("COMPLETED")
                .build();

            transactionRepository.save(transaction);

            // Update SIP totals
            sip.setTotalInvested(sip.getTotalInvested().add(installment.getAmount()));
            sip.setNextDebit(sip.getNextDebit().plusMonths(1));
            sipRepository.save(sip);

        } else {
            // Process failed payment
            payment.setStatus("FAILED");
            payment.setFailureReason(failureReason);
            payment.setProcessedAt(now);
            paymentRepository.save(payment);

            // Update installment status
            installment.setStatus("FAILED");
            sipInstallmentRepository.save(installment);

            // Create failed transaction record
            Transaction transaction = Transaction.builder()
                .user(user)
                .fundName(sip.getFundName())
                .date(installment.getInstallmentDate())
                .amount(installment.getAmount())
                .units(BigDecimal.ZERO)
                .nav(BigDecimal.ZERO)
                .type("BUY")
                .status("FAILED")
                .build();

            transactionRepository.save(transaction);
        }

        return toPaymentResponse(payment);
    }

    /**
     * Handle installment payment when no payment ID is provided.
     * Creates a FAILED transaction record.
     */
    @Transactional
    public void recordFailedInstallmentPayment(UUID userId, UUID sipInstallmentId, String failureReason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        SipInstallment installment = sipInstallmentRepository.findById(sipInstallmentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Installment not found"));

        Sip sip = installment.getSip();

        // Update installment status
        installment.setStatus("FAILED");
        sipInstallmentRepository.save(installment);

        // Create failed transaction record
        Transaction transaction = Transaction.builder()
            .user(user)
            .fundName(sip.getFundName())
            .date(installment.getInstallmentDate())
            .amount(installment.getAmount())
            .units(BigDecimal.ZERO)
            .nav(BigDecimal.ZERO)
            .type("BUY")
            .status("FAILED")
            .build();

        transactionRepository.save(transaction);
    }

    /**
     * Get single payment details
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID userId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        return toPaymentResponse(payment);
    }

    /**
     * Get all payments for a user
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(UUID userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toPaymentResponse)
            .toList();
    }

    /**
     * Get payments by status
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPaymentsByStatus(UUID userId, String status) {
        return paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status.toUpperCase())
            .stream()
            .map(this::toPaymentResponse)
            .toList();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
            .id(payment.getId())
            .amount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod())
            .status(payment.getStatus())
            .description(payment.getDescription())
            .externalPaymentId(payment.getExternalPaymentId())
            .createdAt(payment.getCreatedAt())
            .processedAt(payment.getProcessedAt())
            .failureReason(payment.getFailureReason())
            .build();
    }
}
