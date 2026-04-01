package com.wealthwise.repository;

import com.wealthwise.entity.SipInstallment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SipInstallmentRepository extends JpaRepository<SipInstallment, UUID> {

    List<SipInstallment> findBySipId(UUID sipId);

    List<SipInstallment> findBySipIdAndStatus(UUID sipId, String status);

    List<SipInstallment> findByInstallmentDateAndStatus(LocalDate date, String status);

    List<SipInstallment> findByStatus(String status);
}
