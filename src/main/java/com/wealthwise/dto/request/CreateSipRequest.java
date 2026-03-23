package com.wealthwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateSipRequest {

    @NotBlank(message = "Fund name is required")
    private String fundName;

    @NotNull(message = "Monthly amount is required")
    @DecimalMin(value = "0.01", message = "Monthly amount must be greater than 0")
    private BigDecimal monthlyAmt;
}
