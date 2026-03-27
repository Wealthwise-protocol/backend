package com.wealthwise.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AllocationResponse {

    private String name;
    private String label;
    private BigDecimal value;
}

