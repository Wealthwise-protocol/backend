package com.wealthwise.service;

import com.wealthwise.dto.request.CreateSipRequest;
import com.wealthwise.dto.request.UpdateSipRequest;
import com.wealthwise.dto.response.SipInstallmentResponse;
import com.wealthwise.dto.response.SipResponse;
import com.wealthwise.entity.Sip;
import com.wealthwise.entity.SipInstallment;
import com.wealthwise.entity.User;
import com.wealthwise.repository.SipRepository;
import com.wealthwise.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<SipResponse> getUserSips(UUID userId) {
        return sipRepository.findByUserId(userId).stream()
            .map(this::toSipResponse)
            .toList();
    }

    @Transactional
    public SipResponse createSip(UUID userId, CreateSipRequest request) {
        User user = getUserOrThrow(userId);

        LocalDate today = LocalDate.now();

        Sip sip = Sip.builder()
            .user(user)
            .fundName(request.getFundName())
            .monthlyAmt(request.getMonthlyAmt())
            .startDate(today)
            .nextDebit(today.plusMonths(1))
            .totalInvested(java.math.BigDecimal.ZERO)
            .currentValue(java.math.BigDecimal.ZERO)
            .status("ACTIVE")
            .build();

        Sip saved = sipRepository.save(sip);
        return toSipResponse(saved);
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
            .status(installment.getStatus())
            .build();
    }
}
