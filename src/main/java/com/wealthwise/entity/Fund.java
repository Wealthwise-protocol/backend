package com.wealthwise.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "funds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scheme_code", nullable = false)
    private Integer schemeCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String amc;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String subcategory;

    @Column(columnDefinition = "TEXT")
    private String risk;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal nav;

    @Column(name = "nav_change", precision = 10, scale = 2)
    private BigDecimal navChange;

    @Column(name = "nav_change_percent", precision = 5, scale = 2)
    private BigDecimal navChangePercent;

    @Column(columnDefinition = "TEXT")
    private String aum;

    @Column(name = "expense_ratio", precision = 5, scale = 2)
    private BigDecimal expenseRatio;

    @Column(name = "min_sip")
    private Integer minSip;

    @Column(name = "min_lumpsum")
    private Integer minLumpsum;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, BigDecimal> returns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_avg", columnDefinition = "jsonb")
    private Map<String, BigDecimal> categoryAvg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
