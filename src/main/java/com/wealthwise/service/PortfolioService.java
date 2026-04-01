package com.wealthwise.service;

import com.wealthwise.dto.response.AllocationResponse;
import com.wealthwise.dto.response.HistoryResponse;
import com.wealthwise.dto.response.HoldingResponse;
import com.wealthwise.dto.response.PortfolioSummaryResponse;
import com.wealthwise.entity.Fund;
import com.wealthwise.entity.FundNavHistory;
import com.wealthwise.entity.Holding;
import com.wealthwise.entity.User;
import com.wealthwise.repository.FundNavHistoryRepository;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.repository.HoldingRepository;
import com.wealthwise.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private static final int UNIT_SCALE = 8;
    private static final int NAV_SCALE = 6;
    private static final int MONEY_SCALE = 2;

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final FundRepository fundRepository;
    private final FundNavHistoryRepository fundNavHistoryRepository;
    private final MfApiService mfApiService;

    @Transactional
    public void updateHolding(UUID userId, UUID fundId, BigDecimal amount, BigDecimal nav) {
        createOrUpdateHolding(userId, fundId, amount, nav);
    }

    @Transactional
    public HoldingResponse createOrUpdateHolding(UUID userId, UUID fundId, BigDecimal amount, BigDecimal nav) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Fund fund = fundRepository.findById(fundId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found"));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        BigDecimal purchaseNav = normalizeNav(nav);
        if (purchaseNav == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NAV unavailable for holding update");
        }

        BigDecimal unitsPurchased = safeDivide(amount, purchaseNav, UNIT_SCALE);
        if (unitsPurchased.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Calculated units must be greater than zero");
        }

        Holding holding = holdingRepository.findByUserIdAndFundId(userId, fundId)
            .orElseGet(() -> createInitialHolding(user, fund));

        BigDecimal totalUnits = holding.getUnits().add(unitsPurchased).setScale(UNIT_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalInvested = holding.getInvested().add(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal avgNav = safeDivide(totalInvested, totalUnits, NAV_SCALE);

        BigDecimal currentNav = normalizeNav(fetchLatestNavForFund(fund));
        if (currentNav == null) {
            currentNav = purchaseNav;
        }

        BigDecimal currentValue = totalUnits.multiply(currentNav).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal gain = currentValue.subtract(totalInvested).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        holding.setUnits(totalUnits);
        holding.setInvested(totalInvested);
        holding.setAvgNav(avgNav);
        holding.setCurNav(currentNav);
        holding.setCurValue(currentValue);
        holding.setGain(gain);

        Holding saved = holdingRepository.save(holding);
        return toHoldingResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> getHoldings(UUID userId) {
        return holdingRepository.findByUserId(userId).stream()
            .sorted(Comparator.comparing(Holding::getCurValue).reversed())
            .map(this::toHoldingResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getSummary(UUID userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        if (holdings.isEmpty()) {
            return PortfolioSummaryResponse.builder()
                .totalInvested(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .currentValue(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .totalReturns(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .returnsPercent(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .build();
        }

        BigDecimal invested = holdings.stream()
            .map(Holding::getInvested)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal currentValue = holdings.stream()
            .map(Holding::getCurValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal returns = currentValue.subtract(invested).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal returnsPercent = invested.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            : returns.multiply(BigDecimal.valueOf(100)).divide(invested, MONEY_SCALE, RoundingMode.HALF_UP);

        return PortfolioSummaryResponse.builder()
            .totalInvested(invested)
            .currentValue(currentValue)
            .totalReturns(returns)
            .returnsPercent(returnsPercent)
            .build();
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> getAllocation(UUID userId) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        if (holdings.isEmpty()) {
            return List.of();
        }

        BigDecimal totalValue = holdings.stream()
            .map(Holding::getCurValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = new HashMap<>();
        for (Holding holding : holdings) {
            String category = normalizeCategory(holding.getCategory());
            byCategory.merge(category, holding.getCurValue(), BigDecimal::add);
        }

        List<AllocationResponse> allocation = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : byCategory.entrySet()) {
            BigDecimal percent = totalValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : entry.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(totalValue, MONEY_SCALE, RoundingMode.HALF_UP);
            allocation.add(AllocationResponse.builder()
                .name(entry.getKey())
                .label(entry.getKey())
                .value(percent)
                .build());
        }

        allocation.sort(Comparator.comparing(AllocationResponse::getValue).reversed());
        return allocation;
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> getHistory(UUID userId, String period) {
        List<Holding> holdings = holdingRepository.findByUserId(userId);
        if (holdings.isEmpty()) {
            return List.of();
        }

        LocalDate startDate = calculateStartDate(period);
        Map<LocalDate, BigDecimal> aggregated = new HashMap<>();

        for (Holding holding : holdings) {
            List<FundNavHistory> navPoints = fundNavHistoryRepository
                .findByFundIdAndDateAfter(holding.getFund().getId(), startDate);

            for (FundNavHistory navPoint : navPoints) {
                BigDecimal value = holding.getUnits().multiply(navPoint.getNav())
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                aggregated.merge(navPoint.getDate(), value, BigDecimal::add);
            }
        }

        return aggregated.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> HistoryResponse.builder()
                .month(entry.getKey().toString())
                .value(entry.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .build())
            .toList();
    }

    @Transactional
    public void refreshAllHoldings() {
        List<Holding> holdings = holdingRepository.findAll();
        if (holdings.isEmpty()) {
            return;
        }

        for (Holding holding : holdings) {
            BigDecimal latestNav = normalizeNav(fetchLatestNavForFund(holding.getFund()));
            if (latestNav == null) {
                continue;
            }

            BigDecimal currentValue = holding.getUnits().multiply(latestNav)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal gain = currentValue.subtract(holding.getInvested())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            holding.setCurNav(latestNav);
            holding.setCurValue(currentValue);
            holding.setGain(gain);
        }

        holdingRepository.saveAll(holdings);
    }

    private Holding createInitialHolding(User user, Fund fund) {
        return Holding.builder()
            .user(user)
            .fund(fund)
            .name(fund.getName())
            .category(fund.getCategory())
            .units(BigDecimal.ZERO.setScale(UNIT_SCALE, RoundingMode.HALF_UP))
            .avgNav(BigDecimal.ZERO.setScale(NAV_SCALE, RoundingMode.HALF_UP))
            .curNav(BigDecimal.ZERO.setScale(NAV_SCALE, RoundingMode.HALF_UP))
            .invested(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
            .curValue(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
            .gain(BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP))
            .build();
    }

    private HoldingResponse toHoldingResponse(Holding holding) {
        return HoldingResponse.builder()
            .id(holding.getFund().getId().toString())
            .name(holding.getName())
            .category(holding.getCategory())
            .units(holding.getUnits())
            .avgNav(holding.getAvgNav())
            .curNav(holding.getCurNav())
            .invested(holding.getInvested())
            .curValue(holding.getCurValue())
            .gain(holding.getGain())
            .build();
    }

    private BigDecimal normalizeNav(BigDecimal nav) {
        if (nav == null || nav.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return nav.setScale(NAV_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return numerator.divide(denominator, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal fetchLatestNavForFund(Fund fund) {
        if (fund.getSchemeCode() != null) {
            Map<String, Object> schemeApiResponse = mfApiService.getLatestNav(fund.getSchemeCode().toString());
            BigDecimal nav = extractNav(schemeApiResponse);
            if (nav != null) {
                return nav;
            }
        }

        return fund.getNav();
    }

    private BigDecimal extractNav(Map<String, Object> payload) {
        if (payload == null || payload.get("nav") == null) {
            return null;
        }

        Object navObj = payload.get("nav");
        if (navObj instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        try {
            return new BigDecimal(navObj.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }


    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "Other";
        }
        return category;
    }

    private LocalDate calculateStartDate(String period) {
        LocalDate now = LocalDate.now();
        return switch (period == null ? "1Y" : period.toUpperCase()) {
            case "1M" -> now.minusMonths(1);
            case "3M" -> now.minusMonths(3);
            case "6M" -> now.minusMonths(6);
            case "1Y" -> now.minusYears(1);
            case "ALL" -> LocalDate.of(1900, 1, 1);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid period");
        };
    }
}

