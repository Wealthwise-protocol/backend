package com.wealthwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SipInstallmentResponse {

    private UUID id;
    private LocalDate installmentDate;
    private BigDecimal amount;
    private String status;
}
