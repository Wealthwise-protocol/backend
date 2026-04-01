package com.wealthwise.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sips")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sip {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id")
    private Fund fund;

    @Column(name = "fund_name", nullable = false, length = 255)
    private String fundName;

    @Column(name = "monthly_amt", nullable = false)
    private BigDecimal monthlyAmt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_debit", nullable = false)
    private LocalDate nextDebit;

    @Column(name = "total_invested", nullable = false)
    private BigDecimal totalInvested;

    @Column(name = "current_value", nullable = false)
    private BigDecimal currentValue;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (totalInvested == null) {
            totalInvested = BigDecimal.ZERO;
        }
        if (currentValue == null) {
            currentValue = BigDecimal.ZERO;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
