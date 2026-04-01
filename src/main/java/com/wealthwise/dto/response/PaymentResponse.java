package com.wealthwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;

    private BigDecimal amount;

    private String paymentMethod;

    private String status;

    private String description;

    private String externalPaymentId;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private String failureReason;
}
