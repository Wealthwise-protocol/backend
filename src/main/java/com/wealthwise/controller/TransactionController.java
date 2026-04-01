package com.wealthwise.controller;

import com.wealthwise.dto.response.TransactionResponse;
import com.wealthwise.service.TransactionService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * TransactionController provides endpoints to view payment/debit history for SIPs.
 * 
 * Transactions are automatically created when:
 * - An SIP is created (immediate debit)
 * - Monthly SIP installments are processed
 * 
 * Transactions cannot be manually created via API - use SIP endpoints instead.
 */
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Get all payment/debit transactions for the authenticated user
     * 
     * Query Parameters:
     * - sort: "asc" (ascending) or "desc" (descending, default)
     */
    @GetMapping
    public ResponseEntity<Map<String, List<TransactionResponse>>> getTransactions(
        @AuthenticationPrincipal String userId,
        @RequestParam(name = "sort", required = false, defaultValue = "desc") String sort
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        List<TransactionResponse> transactions = transactionService.getTransactions(userUuid, sort);
        return ResponseEntity.ok(Map.of("transactions", transactions));
    }

    /**
     * Get all payment/debit transactions for a specific fund
     * 
     * Path Variables:
     * - fundName: Name of the fund
     */
    @GetMapping("/fund/{fundName}")
    public ResponseEntity<Map<String, List<TransactionResponse>>> getTransactionsByFund(
        @AuthenticationPrincipal String userId,
        @PathVariable String fundName
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        List<TransactionResponse> transactions = transactionService.getTransactionsByFund(userUuid, fundName);
        return ResponseEntity.ok(Map.of("transactions", transactions));
    }

    private UUID getAuthenticatedUserId(String userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
