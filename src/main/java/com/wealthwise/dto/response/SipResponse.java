package com.wealthwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SipResponse {

    private UUID id;
    private UUID fundId;
    private String fundName;
    private BigDecimal monthlyAmt;
    private LocalDate startDate;
    private LocalDate nextDebit;
    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private String status;
}
