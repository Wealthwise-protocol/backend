package com.wealthwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundResponse {
    private UUID id;
    private Integer schemeCode;
    private String name;
    private String amc;
    private String category;
    private String subcategory;
    private String risk;
    private String description;
    private BigDecimal nav;
    private BigDecimal navChange;
    private BigDecimal navChangePercent;
    private String aum;
    private BigDecimal expenseRatio;
    private Integer minSip;
    private Integer minLumpsum;
    private Map<String, BigDecimal> returns;
    private Map<String, BigDecimal> categoryAvg;
}
