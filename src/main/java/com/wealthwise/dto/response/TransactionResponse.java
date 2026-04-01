package com.wealthwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * TransactionResponse represents a payment/debit record for an SIP investment.
 * This shows the payment history when amounts are debited from user accounts.
 */
@Getter
@Builder
public class TransactionResponse {

    private UUID id;
    private UUID userId;
    private String fundName;
    private LocalDate date;
    private BigDecimal amount;
    private BigDecimal units;
    private BigDecimal nav;
    private String type;
    private String status;
}
