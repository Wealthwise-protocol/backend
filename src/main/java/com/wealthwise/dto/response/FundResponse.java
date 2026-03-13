package com.wealthwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundResponse {
    private String id;
    private String name;
    private String amc;
    private String category;
    private String subcategory;
    private String risk;
    private BigDecimal nav;
    private BigDecimal navChange;
    private BigDecimal navChangePercent;
    private BigDecimal return1y;
    private BigDecimal return3y;
    private BigDecimal return5y;
    private BigDecimal categoryAvg1y;
    private BigDecimal categoryAvg3y;
    private BigDecimal categoryAvg5y;
    private BigDecimal minSip;
    private BigDecimal minLumpsum;
    private BigDecimal aum;
    private BigDecimal expenseRatio;
    private String description;
}
