package com.wealthwise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "holdings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "fund_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fund_id", nullable = false)
    private Fund fund;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "category", length = 120)
    private String category;

    @Column(name = "units", nullable = false, precision = 19, scale = 8)
    private BigDecimal units;

    @Column(name = "avg_nav", nullable = false, precision = 19, scale = 6)
    private BigDecimal avgNav;

    @Column(name = "cur_nav", nullable = false, precision = 19, scale = 6)
    private BigDecimal curNav;

    @Column(name = "invested", nullable = false, precision = 19, scale = 2)
    private BigDecimal invested;

    @Column(name = "cur_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal curValue;

    @Column(name = "gain", nullable = false, precision = 19, scale = 2)
    private BigDecimal gain;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (units == null) {
            units = BigDecimal.ZERO;
        }
        if (avgNav == null) {
            avgNav = BigDecimal.ZERO;
        }
        if (curNav == null) {
            curNav = BigDecimal.ZERO;
        }
        if (invested == null) {
            invested = BigDecimal.ZERO;
        }
        if (curValue == null) {
            curValue = BigDecimal.ZERO;
        }
        if (gain == null) {
            gain = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

