package com.wealthwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavHistoryResponse {
    private List<NavDataPoint> navHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NavDataPoint {
        private LocalDate date;
        private BigDecimal nav;
    }
}
