package com.wealthwise.controller;

import com.wealthwise.dto.response.TransactionResponse;
import com.wealthwise.repository.TransactionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<Map<String, List<TransactionResponse>>> getTransactions(
        @AuthenticationPrincipal String userId
    ) {
        UUID userUuid = getAuthenticatedUserId(userId);
        List<TransactionResponse> transactions = transactionRepository
            .findByUserIdOrderByDateDesc(userUuid)
            .stream()
            .map(t -> TransactionResponse.builder()
                .id(t.getId())
                .date(t.getDate())
                .fundName(t.getFundName())
                .type(t.getType())
                .amount(t.getAmount())
                .nav(t.getNav())
                .units(t.getUnits())
                .status(t.getStatus())
                .build())
            .toList();
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
