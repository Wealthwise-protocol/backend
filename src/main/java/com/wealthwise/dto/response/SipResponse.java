package com.wealthwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SipResponse {

    private UUID id;
    private String fundName;
    private BigDecimal monthlyAmt;
    private LocalDate startDate;
    private LocalDate nextDebit;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private String status;
    private List<SipInstallmentResponse> installments;
}
