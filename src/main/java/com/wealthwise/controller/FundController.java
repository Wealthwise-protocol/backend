package com.wealthwise.controller;

import com.wealthwise.dto.request.FundDetailsRequest;
import com.wealthwise.dto.request.InvestFundRequest;
import com.wealthwise.dto.response.FundResponse;
import com.wealthwise.dto.response.NavHistoryResponse;
import com.wealthwise.dto.response.TransactionResponse;
import com.wealthwise.service.FundService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;

    @GetMapping
   public ResponseEntity<Page<FundResponse>> searchFunds(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        if (size <= 0 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(fundService.searchFunds(search, category, pageable));
    }

    @PostMapping
    public ResponseEntity<FundResponse> getFundDetails(@Valid @RequestBody FundDetailsRequest request) {
        return ResponseEntity.ok(fundService.getFundDetails(request.getId()));
    }

    @GetMapping("/{id}/nav-history")
    public ResponseEntity<NavHistoryResponse> getNavHistory(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "1Y") String period
    ) {
        return ResponseEntity.ok(fundService.getNavHistory(id, period));
    }

    @PostMapping("/{id}/invest")
    public ResponseEntity<Map<String, TransactionResponse>> invest(
        @PathVariable UUID id,
        @Valid @RequestBody InvestFundRequest request,
        Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        String type = request.getType() != null ? request.getType() : "Lumpsum";
        TransactionResponse response = fundService.invest(userId, id, type, request.getAmount());
        return ResponseEntity.ok(Map.of("transaction", response));
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
