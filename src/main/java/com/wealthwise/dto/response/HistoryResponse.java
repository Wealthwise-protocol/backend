package com.wealthwise.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HistoryResponse {

    private String month;
    private BigDecimal value;
}

