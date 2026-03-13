package com.wealthwise.controller;

import com.wealthwise.dto.request.FundDetailsRequest;
import com.wealthwise.dto.request.InvestFundRequest;
import com.wealthwise.dto.response.FundResponse;
import com.wealthwise.dto.response.NavHistoryResponse;
import com.wealthwise.dto.response.SuccessResponse;
import com.wealthwise.service.FundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;

    @GetMapping
    public ResponseEntity<List<FundResponse>> searchFunds(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(fundService.searchFunds(search, category));
    }

    @PostMapping
    public ResponseEntity<FundResponse> getFundDetails(@Valid @RequestBody FundDetailsRequest request) {
        return ResponseEntity.ok(fundService.getFundDetails(request.getId()));
    }

    @GetMapping("/{id}/nav-history")
    public ResponseEntity<NavHistoryResponse> getNavHistory(
        @PathVariable String id,
        @RequestParam(defaultValue = "1Y") String period
    ) {
        return ResponseEntity.ok(fundService.getNavHistory(id, period));
    }

    @PostMapping("/{id}/invest")
    public ResponseEntity<SuccessResponse> invest(
        @PathVariable String id,
        @Valid @RequestBody InvestFundRequest request,
        Authentication authentication
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(fundService.invest(userId, id, request.getType(), request.getAmount()));
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
