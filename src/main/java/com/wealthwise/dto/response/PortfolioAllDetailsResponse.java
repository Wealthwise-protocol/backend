package com.wealthwise.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortfolioAllDetailsResponse {

    private List<HoldingResponse> holdings;
    private List<HistoryResponse> portfolioHistory;
    private List<AllocationResponse> assetAllocation;
    private PortfolioSummaryResponse summary;
}
