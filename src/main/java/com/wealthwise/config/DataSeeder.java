package com.wealthwise.config;

import com.wealthwise.entity.Fund;
import com.wealthwise.entity.FundNavHistory;
import com.wealthwise.repository.FundNavHistoryRepository;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.service.MfApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final FundRepository fundRepository;
    private final FundNavHistoryRepository navHistoryRepository;
    private final MfApiService mfApiService;

    private static final List<String> POPULAR_SCHEMES = List.of(
        "122639", "120503", "125354", "119598", "120465",
        "118989", "118701", "120716", "135781"
    );

    @Override
    public void run(ApplicationArguments args) {
        if (fundRepository.count() == 0) {
            log.info("Seeding initial fund data...");
            POPULAR_SCHEMES.forEach(this::seedFund);
            log.info("Fund seeding completed");
        }
    }

    private void seedFund(String schemeCode) {
        try {
            if (fundRepository.existsById(schemeCode)) {
                return;
            }

            Map<String, Object> apiData = mfApiService.getFundDetails(schemeCode);
            if (apiData == null) {
                log.warn("Failed to fetch data for scheme: {}", schemeCode);
                return;
            }

            Fund fund = Fund.builder()
                .id(schemeCode)
                .name((String) apiData.get("name"))
                .amc((String) apiData.get("amc"))
                .category((String) apiData.get("category"))
                .subcategory((String) apiData.get("subcategory"))
                .nav((BigDecimal) apiData.get("nav"))
                .build();

            Fund persistedFund = fundRepository.saveAndFlush(fund);
            log.info("Seeded fund: {}", persistedFund.getName());

            List<Map<String, Object>> navHistory = (List<Map<String, Object>>) apiData.get("navHistory");
            if (navHistory != null) {
                saveNavHistory(persistedFund, navHistory);
            }
        } catch (Exception e) {
            log.error("Error seeding fund: {}", schemeCode, e);
        }
    }

    private void saveNavHistory(Fund fund, List<Map<String, Object>> navHistory) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        
        List<FundNavHistory> historyToSave = navHistory.stream()
            .limit(365)
            .map(nav -> {
                try {
                    LocalDate date = LocalDate.parse((String) nav.get("date"), formatter);
                    if (!navHistoryRepository.existsByFundIdAndDate(fund.getId(), date)) {
                        return FundNavHistory.builder()
                            .fund(fund)
                            .nav((BigDecimal) nav.get("nav"))
                            .date(date)
                            .build();
                    }
                } catch (Exception e) {
                    // Skip invalid dates
                }
                return null;
            })
            .filter(h -> h != null)
            .collect(Collectors.toList());
        
        if (!historyToSave.isEmpty()) {
            navHistoryRepository.saveAll(historyToSave);
        }
    }
}
