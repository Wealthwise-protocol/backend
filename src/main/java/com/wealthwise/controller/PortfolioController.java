package com.wealthwise.controller;

import com.wealthwise.dto.request.UpsertHoldingRequest;
import com.wealthwise.dto.response.AllocationResponse;
import com.wealthwise.dto.response.HistoryResponse;
import com.wealthwise.dto.response.HoldingResponse;
import com.wealthwise.dto.response.PortfolioAllDetailsResponse;
import com.wealthwise.dto.response.PortfolioSummaryResponse;
import jakarta.validation.Valid;
import com.wealthwise.entity.User;
import com.wealthwise.repository.UserRepository;
import com.wealthwise.service.PortfolioService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    @GetMapping("/all-details")
    public ResponseEntity<PortfolioAllDetailsResponse> getAllDetails(
        @RequestParam(defaultValue = "1Y") String period
    ) {
        UUID userId = getAuthenticatedUserId();
        PortfolioAllDetailsResponse response = PortfolioAllDetailsResponse.builder()
            .holdings(portfolioService.getHoldings(userId))
            .portfolioHistory(portfolioService.getHistory(userId, period))
            .assetAllocation(portfolioService.getAllocation(userId))
            .summary(portfolioService.getSummary(userId))
            .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/holdings")
    public ResponseEntity<Map<String, List<HoldingResponse>>> getHoldings() {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(Map.of("holdings", portfolioService.getHoldings(userId)));
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary() {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(portfolioService.getSummary(userId));
    }

    @GetMapping("/allocation")
    public ResponseEntity<Map<String, List<AllocationResponse>>> getAllocation() {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(Map.of("allocation", portfolioService.getAllocation(userId)));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, List<HistoryResponse>>> getHistory(
        @RequestParam(defaultValue = "1Y") String period
    ) {
        UUID userId = getAuthenticatedUserId();
        return ResponseEntity.ok(Map.of("history", portfolioService.getHistory(userId, period)));
    }

    @PostMapping("/holdings")
    public ResponseEntity<Map<String, HoldingResponse>> upsertHolding(
        @Valid @RequestBody UpsertHoldingRequest request
    ) {
        UUID userId = getAuthenticatedUserId();
        HoldingResponse holding = portfolioService.createOrUpdateHolding(
            userId,
            request.getFundId(),
            request.getAmount(),
            request.getNav()
        );
        return ResponseEntity.ok(Map.of("holding", holding));
    }

    private UUID getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String principal = auth.getName();
        User user = userRepository.findByEmail(principal)
            .orElseGet(() -> {
                try {
                    UUID userId = UUID.fromString(principal);
                    return userRepository.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
                }
            });

        return user.getId();
    }
}

