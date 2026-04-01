package com.wealthwise.repository;

import com.wealthwise.entity.Fund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FundRepository extends JpaRepository<Fund, UUID> {

    @Query(value = """
           SELECT *
           FROM funds f
           WHERE (:search IS NULL OR :search = '' OR CAST(f.name AS TEXT) ILIKE CONCAT('%', :search, '%'))
             AND (:category IS NULL OR :category = '' OR CAST(f.category AS TEXT) = :category)
           ORDER BY f.created_at DESC NULLS LAST
           """, nativeQuery = true)
    Page<Fund> searchFunds(@Param("search") String search, @Param("category") String category, Pageable pageable);

    Optional<Fund> findBySchemeCode(Integer schemeCode);

    boolean existsBySchemeCode(Integer schemeCode);

    long count();
}
