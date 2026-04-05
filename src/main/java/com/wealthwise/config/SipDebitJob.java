package com.wealthwise.config;

import com.wealthwise.entity.Sip;
import com.wealthwise.repository.SipRepository;
import com.wealthwise.service.SipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SipDebitJob {

    private final SipRepository sipRepository;
    private final SipService sipService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void processDueSips() {
        LocalDate today = LocalDate.now();
        List<Sip> dueSips = sipRepository.findByStatusAndNextDebitLessThanEqual("ACTIVE", today);

        log.info("SIP debit job started. Found {} due SIPs", dueSips.size());

        for (Sip sip : dueSips) {
            try {
                sipService.processInstallment(sip);
                log.info("Processed SIP installment for SIP {} (fund: {})", sip.getId(), sip.getFundName());
            } catch (Exception e) {
                log.error("Failed to process SIP installment for SIP {} (fund: {}): {}",
                    sip.getId(), sip.getFundName(), e.getMessage(), e);
            }
        }

        log.info("SIP debit job completed. Processed {} SIPs", dueSips.size());
    }
}
