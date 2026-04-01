package com.wealthwise.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class ProcessPaymentRequest {

    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    @NotNull(message = "SIP ID is required")
    private UUID sipId;

    private UUID sipInstallmentId; // Optional, for installment payments
}
