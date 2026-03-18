package com.wealthwise.repository;

import com.wealthwise.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    @Query("SELECT b.fund.id FROM Bookmark b WHERE b.user.id = :userId")
    List<UUID> findFundIdsByUserId(@Param("userId") UUID userId);

    Optional<Bookmark> findByUserIdAndFundId(UUID userId, UUID fundId);

    @Modifying
    @Transactional
    void deleteByUserIdAndFundId(UUID userId, UUID fundId);
}
