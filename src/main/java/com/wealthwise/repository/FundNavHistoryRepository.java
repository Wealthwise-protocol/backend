package com.wealthwise.repository;

import com.wealthwise.entity.FundNavHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FundNavHistoryRepository extends JpaRepository<FundNavHistory, UUID> {

    @Query("SELECT fnh FROM FundNavHistory fnh WHERE fnh.fund.id = :fundId AND fnh.date >= :startDate ORDER BY fnh.date ASC")
    List<FundNavHistory> findByFundIdAndDateAfter(@Param("fundId") String fundId, @Param("startDate") LocalDate startDate);

    boolean existsByFundIdAndDate(String fundId, LocalDate date);
}
