package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "funds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fund {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false)
    private String name;

    private String amc;
    private String category;
    private String subcategory;
    private String risk;

    @Column(precision = 15, scale = 4)
    private BigDecimal nav;

    @Column(name = "nav_change", precision = 15, scale = 4)
    private BigDecimal navChange;

    @Column(name = "nav_change_percent", precision = 10, scale = 2)
    private BigDecimal navChangePercent;

    @Column(name = "return_1y", precision = 10, scale = 2)
    private BigDecimal return1y;

    @Column(name = "return_3y", precision = 10, scale = 2)
    private BigDecimal return3y;

    @Column(name = "return_5y", precision = 10, scale = 2)
    private BigDecimal return5y;

    @Column(name = "category_avg_1y", precision = 10, scale = 2)
    private BigDecimal categoryAvg1y;

    @Column(name = "category_avg_3y", precision = 10, scale = 2)
    private BigDecimal categoryAvg3y;

    @Column(name = "category_avg_5y", precision = 10, scale = 2)
    private BigDecimal categoryAvg5y;

    @Column(name = "min_sip", precision = 15, scale = 2)
    private BigDecimal minSip;

    @Column(name = "min_lumpsum", precision = 15, scale = 2)
    private BigDecimal minLumpsum;

    @Column(precision = 15, scale = 2)
    private BigDecimal aum;

    @Column(name = "expense_ratio", precision = 10, scale = 2)
    private BigDecimal expenseRatio;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
