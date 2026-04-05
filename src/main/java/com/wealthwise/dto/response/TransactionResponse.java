package com.wealthwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponse {

    private UUID id;
    private LocalDate date;
    private String fundName;
    private String type;
    private BigDecimal amount;
    private BigDecimal nav;
    private BigDecimal units;
    private String status;
}
