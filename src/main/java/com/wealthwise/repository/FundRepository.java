package com.wealthwise.repository;

import com.wealthwise.entity.Fund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FundRepository extends JpaRepository<Fund, String> {

    @Query(value = """
           SELECT *
           FROM funds f
           WHERE (:search IS NULL OR :search = '' OR CAST(f.name AS TEXT) ILIKE CONCAT('%', :search, '%'))
             AND (:category IS NULL OR :category = '' OR CAST(f.category AS TEXT) = :category)
           ORDER BY f.updated_at DESC NULLS LAST, f.created_at DESC NULLS LAST
           """, nativeQuery = true)
    List<Fund> searchFunds(@Param("search") String search, @Param("category") String category);

    long count();
}
