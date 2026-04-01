package com.wealthwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateTransactionRequest {

    @NotNull(message = "Fund ID is required")
    private UUID fundId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Fund name is required")
    private String fundName;

    @NotNull(message = "Type is required")
    private String type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Units is required")
    @DecimalMin(value = "0.01", message = "Units must be greater than 0")
    private BigDecimal units;

    @NotNull(message = "NAV is required")
    @DecimalMin(value = "0.01", message = "NAV must be greater than 0")
    private BigDecimal nav;

    @NotNull(message = "Status is required")
    private String status;
}
