package com.wealthwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateSipRequest {

    @NotNull(message = "SIP id is required")
    private UUID sipId;

    @DecimalMin(value = "0.01", message = "Monthly amount must be greater than 0")
    private BigDecimal monthlyAmt;

    @Pattern(regexp = "ACTIVE|PAUSED", message = "Status must be either ACTIVE or PAUSED")
    private String status;
}
