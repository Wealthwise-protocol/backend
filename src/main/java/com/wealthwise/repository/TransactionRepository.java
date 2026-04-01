package com.wealthwise.repository;

import com.wealthwise.entity.Transaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find all payment/debit transactions for a user
     */
    List<Transaction> findByUserId(UUID userId, Sort sort);

    /**
     * Find all transactions for a user for a specific fund
     */
    List<Transaction> findByUserIdAndFundName(UUID userId, String fundName, Sort sort);
}
