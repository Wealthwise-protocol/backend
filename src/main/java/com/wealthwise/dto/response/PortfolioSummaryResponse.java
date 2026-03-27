package com.wealthwise.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortfolioSummaryResponse {

    private BigDecimal totalInvested;
    private BigDecimal currentValue;
    private BigDecimal totalReturns;
    private BigDecimal returnsPercent;
}

