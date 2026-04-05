package com.wealthwise.service;

import com.wealthwise.dto.response.BookmarkResponse;
import com.wealthwise.dto.response.FundResponse;
import com.wealthwise.dto.response.NavHistoryResponse;
import com.wealthwise.dto.response.SuccessResponse;
import com.wealthwise.dto.response.TransactionResponse;
import com.wealthwise.entity.Bookmark;
import com.wealthwise.entity.Fund;
import com.wealthwise.entity.FundNavHistory;
import com.wealthwise.entity.User;
import com.wealthwise.entity.Transaction;
import com.wealthwise.repository.BookmarkRepository;
import com.wealthwise.repository.FundNavHistoryRepository;
import com.wealthwise.repository.FundRepository;
import com.wealthwise.repository.TransactionRepository;
import com.wealthwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    private final TransactionRepository transactionRepository;
    private final MfApiService mfApiService;
    private final PortfolioService portfolioService;

    @Transactional
    public Page<FundResponse> searchFunds(String search, String category, Pageable pageable) {
        Page<Fund> funds = fundRepository.searchFunds(search, category, pageable);
        
        if (funds.isEmpty() && search != null && !search.isBlank() && pageable.getPageNumber() == 0) {
            List<Map<String, Object>> apiResults = mfApiService.searchFunds(search);
            List<FundResponse> fetchedFunds = apiResults.stream()
                .limit(10)
                .map(result -> {
                    Integer schemeCode = Integer.parseInt((String) result.get("id"));
                    Fund fund = fundRepository.findBySchemeCode(schemeCode)
                        .orElseGet(() -> fetchAndSaveFund(schemeCode));
                    return toFundResponse(fund);
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());

            int start = Math.min((int) pageable.getOffset(), fetchedFunds.size());
            int end = Math.min(start + pageable.getPageSize(), fetchedFunds.size());
            List<FundResponse> pageContent = fetchedFunds.subList(start, end);

            return new PageImpl<>(pageContent, pageable, fetchedFunds.size());
        }
        
        return funds.map(this::toFundResponse);
    }

    @Transactional
    public FundResponse getFundDetails(UUID id) {
        Fund fund = fundRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found"));
        
        return toFundResponse(fund);
    }

    @Transactional(readOnly = true)
    public NavHistoryResponse getNavHistory(UUID fundId, String period) {
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
        
        return NavHistoryResponse.builder().navHistory(dataPoints).build();
    }

    @Transactional
    public TransactionResponse invest(UUID userId, UUID fundId, String type, BigDecimal amount) {
        Fund fund = fundRepository.findById(fundId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fund not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        
        if (type == null || (!type.equals("SIP") && !type.equals("Lumpsum"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type must be either 'SIP' or 'Lumpsum'");
        }
        
        if (type.equals("SIP") && fund.getMinSip() != null) {
            BigDecimal minSipAmount = new BigDecimal(fund.getMinSip());
            if (amount.compareTo(minSipAmount) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Amount must be at least " + fund.getMinSip() + " for SIP");
            }
        }
        
        if (type.equals("Lumpsum") && fund.getMinLumpsum() != null) {
            BigDecimal minLumpsumAmount = new BigDecimal(fund.getMinLumpsum());
            if (amount.compareTo(minLumpsumAmount) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Amount must be at least " + fund.getMinLumpsum() + " for Lumpsum");
            }
        }
        
        BigDecimal latestNav = resolveLatestNav(fund);
        BigDecimal units = amount.divide(latestNav, 8, java.math.RoundingMode.HALF_UP);

        portfolioService.updateHolding(userId, fund.getId(), amount, latestNav);

        Transaction transaction = Transaction.builder()
            .user(user)
            .fund(fund)
            .fundName(fund.getName())
            .type(type)
            .date(LocalDate.now())
            .amount(amount)
            .nav(latestNav)
            .units(units)
            .status("Success")
            .build();
        Transaction saved = transactionRepository.save(transaction);

        return TransactionResponse.builder()
            .id(saved.getId())
            .date(saved.getDate())
            .fundName(saved.getFundName())
            .type(saved.getType())
            .amount(saved.getAmount())
            .nav(saved.getNav())
            .units(saved.getUnits())
            .status(saved.getStatus())
            .build();
    }

    @Transactional(readOnly = true)
    public BookmarkResponse getBookmarks(UUID userId) {
        List<UUID> fundIds = bookmarkRepository.findFundIdsByUserId(userId);
        return BookmarkResponse.builder().fundIds(fundIds).build();
    }

    @Transactional
    public SuccessResponse addBookmark(UUID userId, UUID fundId) {
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
    public SuccessResponse removeBookmark(UUID userId, UUID fundId) {
        bookmarkRepository.deleteByUserIdAndFundId(userId, fundId);
        return SuccessResponse.builder().success(true).build();
    }

    private Fund fetchAndSaveFund(Integer schemeCode) {
        Map<String, Object> apiData = mfApiService.getFundDetails(schemeCode.toString());
        if (apiData == null) return null;
        
        Fund fund = Fund.builder()
            .schemeCode(schemeCode)
            .name((String) apiData.get("name"))
            .amc((String) apiData.get("amc"))
            .category((String) apiData.get("category"))
            .subcategory((String) apiData.get("subcategory"))
            .nav((BigDecimal) apiData.get("nav"))
            .build();
        
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
            .schemeCode(fund.getSchemeCode())
            .name(fund.getName())
            .amc(fund.getAmc())
            .category(fund.getCategory())
            .subcategory(fund.getSubcategory())
            .risk(fund.getRisk())
            .description(fund.getDescription())
            .nav(fund.getNav())
            .navChange(fund.getNavChange())
            .navChangePercent(fund.getNavChangePercent())
            .aum(fund.getAum())
            .expenseRatio(fund.getExpenseRatio())
            .minSip(fund.getMinSip())
            .minLumpsum(fund.getMinLumpsum())
            .returns(fund.getReturns())
            .categoryAvg(fund.getCategoryAvg())
            .build();
    }

    private BigDecimal resolveLatestNav(Fund fund) {
        if (fund.getSchemeCode() != null) {
            Map<String, Object> latestNav = mfApiService.getLatestNav(fund.getSchemeCode().toString());
            if (latestNav != null && latestNav.get("nav") != null) {
                Object navValue = latestNav.get("nav");
                if (navValue instanceof BigDecimal bigDecimal) {
                    return bigDecimal;
                }
                try {
                    return new BigDecimal(navValue.toString());
                } catch (NumberFormatException ignored) {
                    // Fall back to stored NAV.
                }
            }
        }

        if (fund.getNav() == null || fund.getNav().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NAV unavailable for investment");
        }
        return fund.getNav();
    }
}
