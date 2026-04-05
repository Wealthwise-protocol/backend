package com.wealthwise.entity;

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
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id")
    private Fund fund;

    @Column(name = "fund_name", nullable = false)
    private String fundName;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate date;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "nav", precision = 19, scale = 6)
    private BigDecimal nav;

    @Column(name = "units", precision = 19, scale = 8)
    private BigDecimal units;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
