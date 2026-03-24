package com.wealthwise.dto.request;

import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateSipRequest {

    @NotNull(message = "Fund id is required")
    private UUID fundId;

    @NotNull(message = "Monthly amount is required")
    @DecimalMin(value = "0.01", message = "Monthly amount must be greater than 0")
    private BigDecimal monthlyAmt;
}
