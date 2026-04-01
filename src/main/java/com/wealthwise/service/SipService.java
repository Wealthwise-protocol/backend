package com.wealthwise.service;

import com.wealthwise.dto.request.CreateSipRequest;
import com.wealthwise.dto.request.UpdateSipRequest;
import com.wealthwise.dto.response.SipResponse;
import com.wealthwise.entity.Fund;
import com.wealthwise.entity.Payment;
import com.wealthwise.entity.Sip;
import com.wealthwise.entity.SipInstallment;
import com.wealthwise.entity.Transaction;
import com.wealthwise.entity.User;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.repository.PaymentRepository;
import com.wealthwise.repository.SipInstallmentRepository;
import com.wealthwise.repository.SipRepository;
import com.wealthwise.repository.TransactionRepository;
import com.wealthwise.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SipService {

    private final SipRepository sipRepository;
    private final UserRepository userRepository;
    private final FundRepository fundRepository;
    private final PaymentRepository paymentRepository;
    private final SipInstallmentRepository sipInstallmentRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final PaymentService paymentService;
    private final MfApiService mfApiService;
    private final PortfolioService portfolioService;

    @Transactional(readOnly = true)
    public List<SipResponse> getUserSips(UUID userId) {
        return sipRepository.findByUserId(userId).stream()
            .map(this::toSipResponse)
            .toList();
    }

    /**
     * Create SIP with mandatory payment.
     * - Validates payment exists and is in PENDING status
     * - Creates SIP record
     * - Processes payment
     * - Creates transaction record (completed if payment succeeds, failed if payment fails)
     */
    @Transactional
    public SipResponse createSip(UUID userId, CreateSipRequest request) {
        User user = getUserOrThrow(userId);
        Fund fund = fundRepository.findById(request.getFundId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found"));

        // Validate payment exists and belongs to user
        Payment payment = paymentRepository.findByIdAndUserId(request.getPaymentId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid or non-existent payment"));

        // Check payment is in PENDING status
        if (!"PENDING".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Payment must be in PENDING status. Current status: " + payment.getStatus());
        }

        // Check payment amount matches SIP monthly amount
        if (payment.getAmount().compareTo(request.getMonthlyAmt()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Payment amount must match the monthly SIP amount. Payment: " + payment.getAmount() + ", SIP: " + request.getMonthlyAmt());
        }

        LocalDate today = LocalDate.now();
        BigDecimal nav = resolveNavForInvestment(fund);

        // Create SIP
        Sip sip = Sip.builder()
            .user(user)
            .fund(fund)
            .fundName(fund.getName())
            .monthlyAmt(request.getMonthlyAmt())
            .startDate(today)
            .nextDebit(today.plusMonths(1))
            .totalInvested(java.math.BigDecimal.ZERO)
            .currentValue(java.math.BigDecimal.ZERO)
            .status("ACTIVE")
            .build();

        Sip savedSip = sipRepository.save(sip);

        // Process payment (mark as SUCCESS and create transaction)
        // In a real scenario, this would call a payment gateway
        processSuccessfulPayment(user, payment, savedSip, nav);

        // Update portfolio holding
        portfolioService.updateHolding(
            userId,
            fund.getId(),
            request.getMonthlyAmt(),
            nav
        );

        // Create initial SipInstallment for next month
        createNextInstallment(savedSip);

        return toSipResponse(savedSip);
    }

    /**
     * Process successful payment: update payment status and create completed transaction
     */
    private void processSuccessfulPayment(User user, Payment payment, Sip sip, BigDecimal nav) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // Update payment status
        payment.setStatus("SUCCESS");
        payment.setProcessedAt(now);
        paymentRepository.save(payment);

        // Calculate units and create transaction
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
    }

    /**
     * Create initial SIP installment for next month
     */
    private void createNextInstallment(Sip sip) {
        SipInstallment installment = SipInstallment.builder()
            .sip(sip)
            .installmentDate(sip.getNextDebit())
            .amount(sip.getMonthlyAmt())
            .status("PENDING")
            .build();

        sipInstallmentRepository.save(installment);
    }

    @Transactional
    public SipResponse updateSip(UUID userId, UpdateSipRequest request) {
        Sip sip = sipRepository.findByIdAndUserId(request.getSipId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIP not found"));

        if (request.getMonthlyAmt() != null) {
            sip.setMonthlyAmt(request.getMonthlyAmt());
        }

        if (request.getStatus() != null) {
            sip.setStatus(request.getStatus().toUpperCase());
        }

        Sip updated = sipRepository.save(sip);
        return toSipResponse(updated);
    }

    @Transactional
    public void deleteSip(UUID userId, UUID sipId) {
        Sip sip = sipRepository.findByIdAndUserId(sipId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIP not found"));

        sipRepository.delete(sip);
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private SipResponse toSipResponse(Sip sip) {
        return SipResponse.builder()
            .id(sip.getId())
            .fundId(sip.getFund() != null ? sip.getFund().getId() : null)
            .fundName(sip.getFundName())
            .monthlyAmt(sip.getMonthlyAmt())
            .startDate(sip.getStartDate())
            .nextDebit(sip.getNextDebit())
            .totalInvested(sip.getTotalInvested())
            .currentValue(sip.getCurrentValue())
            .status(sip.getStatus())
            .build();
    }

    private BigDecimal resolveNavForInvestment(Fund fund) {
        if (fund.getSchemeCode() != null) {
            Map<String, Object> latestNav = mfApiService.getLatestNav(fund.getSchemeCode().toString());
            if (latestNav != null && latestNav.get("nav") != null) {
                Object nav = latestNav.get("nav");
                if (nav instanceof BigDecimal value) {
                    return value;
                }
                try {
                    return new BigDecimal(nav.toString());
                } catch (NumberFormatException ignored) {
                    // Fall back to persisted NAV.
                }
            }
        }

        return fund.getNav();
    }
}
