package com.wealthwise.config;

import com.wealthwise.entity.Fund;
import com.wealthwise.entity.FundNavHistory;
import com.wealthwise.entity.Holding;
import com.wealthwise.entity.Sip;
import com.wealthwise.repository.FundNavHistoryRepository;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.repository.HoldingRepository;
import com.wealthwise.repository.SipRepository;
import com.wealthwise.service.MfApiService;
import com.wealthwise.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NavSyncJob {

    private final FundRepository fundRepository;
    private final FundNavHistoryRepository navHistoryRepository;
    private final MfApiService mfApiService;
    private final PortfolioService portfolioService;
    private final SipRepository sipRepository;
    private final HoldingRepository holdingRepository;

    @Scheduled(cron = "0 0 19 * * MON-FRI", zone = "Asia/Kolkata")
    public void syncNavData() {
        log.info("Starting NAV sync job...");
        
        int pageSize = 50;
        int pageNumber = 0;
        Page<Fund> fundPage;
        int totalProcessed = 0;
        
        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            fundPage = fundRepository.findAll(pageable);
            
            fundPage.getContent().forEach(fund -> {
                try {
                    Map<String, Object> latestNav = mfApiService.getLatestNav(fund.getSchemeCode().toString());
                    if (latestNav != null) {
                        BigDecimal nav = (BigDecimal) latestNav.get("nav");
                        String dateStr = (String) latestNav.get("date");
                        
                        fund.setNav(nav);
                        fundRepository.save(fund);
                        
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDate date = LocalDate.parse(dateStr, formatter);
                        
                        if (!navHistoryRepository.existsByFundIdAndDate(fund.getId(), date)) {
                            FundNavHistory history = FundNavHistory.builder()
                                .fund(fund)
                                .nav(nav)
                                .date(date)
                                .build();
                            navHistoryRepository.save(history);
                        }
                        
                        log.debug("Updated NAV for fund: {}", fund.getName());
                    }
                } catch (Exception e) {
                    log.error("Error updating NAV for fund: {}", fund.getSchemeCode(), e);
                }
            });
            
            totalProcessed += fundPage.getNumberOfElements();
            pageNumber++;
            
        } while (fundPage.hasNext());

        portfolioService.refreshAllHoldings();
        refreshSipCurrentValues();

        log.info("NAV sync job completed. Processed {} funds", totalProcessed);
    }

    private void refreshSipCurrentValues() {
        List<Sip> activeSips = sipRepository.findByStatus("ACTIVE");

        for (Sip sip : activeSips) {
            try {
                if (sip.getFund() == null || sip.getUser() == null) continue;
                if (sip.getTotalInvested().compareTo(BigDecimal.ZERO) <= 0) continue;

                holdingRepository.findByUserIdAndFundId(sip.getUser().getId(), sip.getFund().getId())
                    .ifPresent(holding -> {
                        if (holding.getInvested().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal ratio = sip.getTotalInvested()
                                .divide(holding.getInvested(), 6, RoundingMode.HALF_UP);
                            sip.setCurrentValue(holding.getCurValue().multiply(ratio)
                                .setScale(2, RoundingMode.HALF_UP));
                            sipRepository.save(sip);
                        }
                    });
            } catch (Exception e) {
                log.error("Error refreshing SIP currentValue for SIP {}: {}", sip.getId(), e.getMessage());
            }
        }
        log.info("Refreshed currentValue for {} active SIPs", activeSips.size());
    }
}
