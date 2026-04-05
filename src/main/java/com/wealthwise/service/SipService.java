package com.wealthwise.service;

import com.wealthwise.dto.request.CreateSipRequest;
import com.wealthwise.dto.request.UpdateSipRequest;
import com.wealthwise.dto.response.SipInstallmentResponse;
import com.wealthwise.dto.response.SipResponse;
import com.wealthwise.entity.Fund;
import com.wealthwise.entity.Sip;
import com.wealthwise.entity.SipInstallment;
import com.wealthwise.entity.User;
import com.wealthwise.entity.Transaction;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.repository.SipRepository;
import com.wealthwise.repository.TransactionRepository;
import com.wealthwise.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final TransactionRepository transactionRepository;
    private final MfApiService mfApiService;
    private final PortfolioService portfolioService;

    @Transactional(readOnly = true)
    public List<SipResponse> getUserSips(UUID userId) {
        return sipRepository.findByUserId(userId).stream()
            .map(this::toSipResponse)
            .toList();
    }

    @Transactional
    public SipResponse createSip(UUID userId, CreateSipRequest request) {
        User user = getUserOrThrow(userId);
        Fund fund = fundRepository.findById(request.getFundId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found"));

        LocalDate today = LocalDate.now();

        Sip sip = Sip.builder()
            .user(user)
            .fund(fund)
            .fundName(fund.getName())
            .monthlyAmt(request.getMonthlyAmt())
            .startDate(today)
            .nextDebit(today)
            .totalInvested(BigDecimal.ZERO)
            .currentValue(BigDecimal.ZERO)
            .status("ACTIVE")
            .build();

        Sip saved = sipRepository.save(sip);
        processInstallment(saved);
        return toSipResponse(saved);
    }

    @Transactional
    public void processInstallment(Sip sip) {
        BigDecimal amount = sip.getMonthlyAmt();
        Fund fund = sip.getFund();
        UUID userId = sip.getUser().getId();

        BigDecimal nav = resolveNavForInvestment(fund);
        BigDecimal units = (nav != null && nav.compareTo(BigDecimal.ZERO) > 0)
            ? amount.divide(nav, 8, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        SipInstallment installment = SipInstallment.builder()
            .sip(sip)
            .installmentDate(LocalDate.now())
            .amount(amount)
            .nav(nav)
            .units(units)
            .status("COMPLETED")
            .build();
        sip.getInstallments().add(installment);

        sip.setTotalInvested(sip.getTotalInvested().add(amount));

        portfolioService.updateHolding(userId, fund.getId(), amount, nav);

        Transaction transaction = Transaction.builder()
            .user(sip.getUser())
            .fund(fund)
            .fundName(fund.getName())
            .type("SIP")
            .date(LocalDate.now())
            .amount(amount)
            .nav(nav)
            .units(units)
            .status("Success")
            .build();
        transactionRepository.save(transaction);

        sip.setNextDebit(sip.getNextDebit().plusMonths(1));
        sip.setCurrentValue(sip.getTotalInvested());

        sipRepository.save(sip);
    }

    @Transactional
    public SipResponse updateSip(UUID userId, UpdateSipRequest request) {
        Sip sip = sipRepository.findByIdAndUserId(request.getSipId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIP not found"));

        if ("CANCELLED".equals(sip.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update a cancelled SIP");
        }

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
    public SipResponse deleteSip(UUID userId, UUID sipId) {
        Sip sip = sipRepository.findByIdAndUserId(sipId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SIP not found"));

        sip.setStatus("CANCELLED");
        Sip saved = sipRepository.save(sip);
        return toSipResponse(saved);
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
            .installments(sip.getInstallments().stream().map(this::toInstallmentResponse).toList())
            .build();
    }

    private SipInstallmentResponse toInstallmentResponse(SipInstallment installment) {
        return SipInstallmentResponse.builder()
            .id(installment.getId())
            .installmentDate(installment.getInstallmentDate())
            .amount(installment.getAmount())
            .nav(installment.getNav())
            .units(installment.getUnits())
            .status(installment.getStatus())
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
