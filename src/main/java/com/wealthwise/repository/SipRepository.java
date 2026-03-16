package com.wealthwise.repository;

import com.wealthwise.entity.Sip;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SipRepository extends JpaRepository<Sip, UUID> {

    List<Sip> findByUserId(UUID userId);

    Optional<Sip> findByIdAndUserId(UUID id, UUID userId);
}
