package com.wealthwise.repository;

import com.wealthwise.entity.SipInstallment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SipInstallmentRepository extends JpaRepository<SipInstallment, UUID> {
}
