package com.wealthwise.repository;

import com.wealthwise.entity.Holding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    List<Holding> findByUserId(UUID userId);

    Optional<Holding> findByUserIdAndFundId(UUID userId, UUID fundId);
}

