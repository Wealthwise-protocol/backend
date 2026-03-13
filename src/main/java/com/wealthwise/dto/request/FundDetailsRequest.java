package com.wealthwise.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FundDetailsRequest {
    @NotBlank(message = "Fund id is required")
    private String id;
}
