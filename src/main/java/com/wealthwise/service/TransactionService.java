package com.wealthwise.service;

import com.wealthwise.dto.response.TransactionResponse;
import com.wealthwise.entity.Transaction;
import com.wealthwise.entity.User;
import com.wealthwise.repository.TransactionRepository;
import com.wealthwise.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * TransactionService manages payment/debit history for SIPs.
 * This service records when amounts are debited from user accounts for SIP investments.
 * Transactions are automatically created when:
 * 1. An SIP is created (immediate debit)
 * 2. Monthly SIP installments are processed
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "date");

    /**
     * Get all payment/debit transactions for a user
     * Sorted by date (newest first by default)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(
        UUID userId,
        String sort
    ) {
        Sort sortOrder = parseSortOrder(sort);
        List<Transaction> transactions = transactionRepository.findByUserId(userId, sortOrder);
        
        return transactions.stream()
            .map(this::toTransactionResponse)
            .toList();
    }

    /**
     * Get all transactions for a fund (by fund_name)
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByFund(UUID userId, String fundName) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndFundName(userId, fundName,
            Sort.by(Sort.Direction.DESC, "date"));
        
        return transactions.stream()
            .map(this::toTransactionResponse)
            .toList();
    }

    /**
     * Internal method to record a payment/debit transaction for SIP
     * Called automatically when:
     * - SIP is created (immediate debit)
     * - Monthly SIP installment is processed
     */
    @Transactional
    public TransactionResponse recordPayment(
        UUID userId,
        String fundName,
        LocalDate paymentDate,
        BigDecimal amount,
        BigDecimal nav
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Calculate units purchased
        BigDecimal unitsPurchased = amount.divide(nav, 2, java.math.RoundingMode.HALF_UP);

        Transaction transaction = Transaction.builder()
            .user(user)
            .fundName(fundName)
            .date(paymentDate)
            .amount(amount)
            .units(unitsPurchased)
            .nav(nav)
            .type("BUY")
            .status("COMPLETED")
            .build();

        Transaction saved = transactionRepository.save(transaction);
        return toTransactionResponse(saved);
    }

    private Sort parseSortOrder(String sort) {
        if (sort != null && sort.equalsIgnoreCase("asc")) {
            return Sort.by(Sort.Direction.ASC, "date");
        }
        return DEFAULT_SORT;
    }

    private TransactionResponse toTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .userId(transaction.getUser().getId())
            .fundName(transaction.getFundName())
            .date(transaction.getDate())
            .amount(transaction.getAmount())
            .units(transaction.getUnits())
            .nav(transaction.getNav())
            .type(transaction.getType())
            .status(transaction.getStatus())
            .build();
    }
}
