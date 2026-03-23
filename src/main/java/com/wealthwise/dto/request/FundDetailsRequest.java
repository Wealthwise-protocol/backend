package com.wealthwise.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class FundDetailsRequest {
    @NotNull(message = "Fund id is required")
    private UUID id;
}
