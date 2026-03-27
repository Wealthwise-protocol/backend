package com.wealthwise.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HoldingResponse {

    private String id;
    private String name;
    private String category;
    private BigDecimal units;
    private BigDecimal avgNav;
    private BigDecimal curNav;
    private BigDecimal invested;
    private BigDecimal curValue;
    private BigDecimal gain;
}

