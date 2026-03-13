package com.wealthwise.service;

import com.wealthwise.dto.response.BookmarkResponse;
import com.wealthwise.dto.response.FundResponse;
import com.wealthwise.dto.response.NavHistoryResponse;
import com.wealthwise.dto.response.SuccessResponse;
import com.wealthwise.entity.Bookmark;
import com.wealthwise.entity.Fund;
import com.wealthwise.entity.FundNavHistory;
import com.wealthwise.entity.User;
import com.wealthwise.repository.BookmarkRepository;
import com.wealthwise.repository.FundNavHistoryRepository;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundService {

    private final FundRepository fundRepository;
    private final FundNavHistoryRepository navHistoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final MfApiService mfApiService;

    @Transactional
    public List<FundResponse> searchFunds(String search, String category) {
        List<Fund> funds = fundRepository.searchFunds(search, category);
        
        if (funds.isEmpty() && search != null && !search.isBlank()) {
            List<Map<String, Object>> apiResults = mfApiService.searchFunds(search);
            return apiResults.stream()
                .limit(10)
                .map(result -> {
                    String schemeCode = (String) result.get("id");
                    Fund fund = fundRepository.findById(schemeCode)
                        .orElseGet(() -> fetchAndSaveFund(schemeCode));
                    return toFundResponse(fund);
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());
        }
        
        return funds.stream().map(this::toFundResponse).collect(Collectors.toList());
    }

    @Transactional
    public FundResponse getFundDetails(String id) {
        Fund fund = fundRepository.findById(id)
            .orElseGet(() -> fetchAndSaveFund(id));
        
        if (fund == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found");
        }
        
        return toFundResponse(fund);
    }

    @Transactional(readOnly = true)
    public NavHistoryResponse getNavHistory(String fundId, String period) {
        if (!fundRepository.existsById(fundId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found");
        }
        
        LocalDate startDate = calculateStartDate(period);
        List<FundNavHistory> history = navHistoryRepository.findByFundIdAndDateAfter(fundId, startDate);
        
        List<NavHistoryResponse.NavDataPoint> dataPoints = history.stream()
            .map(h -> NavHistoryResponse.NavDataPoint.builder()
                .date(h.getDate())
                .nav(h.getNav())
                .build())
            .collect(Collectors.toList());
        
        return NavHistoryResponse.builder().data(dataPoints).build();
    }

    @Transactional
    public SuccessResponse invest(UUID userId, String fundId, String type, BigDecimal amount) {
        Fund fund = fundRepository.findById(fundId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Validate investment parameters
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        
        if (type == null || (!type.equals("SIP") && !type.equals("Lumpsum"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type must be either 'SIP' or 'Lumpsum'");
        }
        
        if (type.equals("SIP") && fund.getMinSip() != null && amount.compareTo(fund.getMinSip()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Amount must be at least " + fund.getMinSip() + " for SIP");
        }
        
        if (type.equals("Lumpsum") && fund.getMinLumpsum() != null && amount.compareTo(fund.getMinLumpsum()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Amount must be at least " + fund.getMinLumpsum() + " for Lumpsum");
        }
        
        // TODO: Atomic transaction implementation:
        // 1. Create Transaction record
        // 2. Update/Create Holding record
        // 3. If type = SIP, create SIP schedule
        
        return SuccessResponse.builder().success(true).build();
    }

    @Transactional(readOnly = true)
    public BookmarkResponse getBookmarks(UUID userId) {
        List<String> fundIds = bookmarkRepository.findFundIdsByUserId(userId);
        return BookmarkResponse.builder().fundIds(fundIds).build();
    }

    @Transactional
    public SuccessResponse addBookmark(UUID userId, String fundId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Fund fund = fundRepository.findById(fundId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found"));
        
        if (bookmarkRepository.findByUserIdAndFundId(userId, fundId).isEmpty()) {
            Bookmark bookmark = Bookmark.builder()
                .user(user)
                .fund(fund)
                .build();
            bookmarkRepository.save(bookmark);
        }
        
        return SuccessResponse.builder().success(true).build();
    }

    @Transactional
    public SuccessResponse removeBookmark(UUID userId, String fundId) {
        bookmarkRepository.deleteByUserIdAndFundId(userId, fundId);
        return SuccessResponse.builder().success(true).build();
    }

    private Fund fetchAndSaveFund(String schemeCode) {
        Map<String, Object> apiData = mfApiService.getFundDetails(schemeCode);
        if (apiData == null) return null;
        
        Fund fund = Fund.builder()
            .id(schemeCode)
            .name((String) apiData.get("name"))
            .amc((String) apiData.get("amc"))
            .category((String) apiData.get("category"))
            .subcategory((String) apiData.get("subcategory"))
            .nav((BigDecimal) apiData.get("nav"))
            .build();
        
        // saveAndFlush ensures we use a managed/persisted fund before linking NAV history rows
        Fund persistedFund = fundRepository.saveAndFlush(fund);
        
        List<Map<String, Object>> navHistory = (List<Map<String, Object>>) apiData.get("navHistory");
        if (navHistory != null) {
            saveNavHistory(persistedFund, navHistory);
        }
        
        return persistedFund;
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

    private LocalDate calculateStartDate(String period) {
        LocalDate now = LocalDate.now();
        return switch (period) {
            case "1M" -> now.minusMonths(1);
            case "3M" -> now.minusMonths(3);
            case "6M" -> now.minusMonths(6);
            case "1Y" -> now.minusYears(1);
            default -> now.minusYears(10);
        };
    }

    private FundResponse toFundResponse(Fund fund) {
        if (fund == null) return null;
        return FundResponse.builder()
            .id(fund.getId())
            .name(fund.getName())
            .amc(fund.getAmc())
            .category(fund.getCategory())
            .subcategory(fund.getSubcategory())
            .risk(fund.getRisk())
            .nav(fund.getNav())
            .navChange(fund.getNavChange())
            .navChangePercent(fund.getNavChangePercent())
            .return1y(fund.getReturn1y())
            .return3y(fund.getReturn3y())
            .return5y(fund.getReturn5y())
            .categoryAvg1y(fund.getCategoryAvg1y())
            .categoryAvg3y(fund.getCategoryAvg3y())
            .categoryAvg5y(fund.getCategoryAvg5y())
            .minSip(fund.getMinSip())
            .minLumpsum(fund.getMinLumpsum())
            .aum(fund.getAum())
            .expenseRatio(fund.getExpenseRatio())
            .description(fund.getDescription())
            .build();
    }
}
