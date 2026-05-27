package com.example.dividend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(
    name = "stock",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_stock_user_code",
        columnNames = {"user_id", "stock_code"}
    )
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    // DB에 enum name(영문)을 VARCHAR로 저장
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StockSector sector;

    @Column(length = 20)
    private String exchange;

    @Column(length = 5)
    private String currency;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "avg_price", precision = 18, scale = 4)
    private BigDecimal avgPrice = BigDecimal.ZERO;

    @Column(name = "previous_close", precision = 18, scale = 4)
    private BigDecimal previousClose = BigDecimal.ZERO;

    /** 배당 주기: ANNUAL / QUARTERLY / MONTHLY */
    @Column(name = "dividend_cycle", length = 20)
    private String dividendCycle;

    /** 주당 예상 배당금 */
    @Column(name = "expected_dividend_per_share")
    private Integer expectedDividendPerShare;

    /**
     * 마지막 전일 종가의 데이터 출처.
     * "yfinance" | "cache" | "avg_purchase" | "unavailable" | null(미업데이트)
     */
    @Column(name = "price_source", length = 20)
    private String priceSource;
}
