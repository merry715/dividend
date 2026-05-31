package com.example.dividend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "dividend",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_stock_year_month",
        columnNames = {"user_id", "stock_id", "year", "month"}
    )
)
public class Dividend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "expected_amount", precision = 15, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "confirmed_amount", precision = 15, scale = 2)
    private BigDecimal confirmedAmount;

    @Column(name = "ex_dividend_date")
    private LocalDate exDividendDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(nullable = false, length = 20)
    private String status = "EXPECTED";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── getters ────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public Long getUserId()              { return userId; }
    public Long getStockId()             { return stockId; }
    public int  getYear()                { return year; }
    public int  getMonth()               { return month; }
    public BigDecimal getExpectedAmount(){ return expectedAmount; }
    public BigDecimal getConfirmedAmount(){ return confirmedAmount; }
    public LocalDate getExDividendDate() { return exDividendDate; }
    public LocalDate getPaymentDate()    { return paymentDate; }
    public String getStatus()            { return status; }

    // ── setters ────────────────────────────────────────────────

    public void setUserId(Long userId)                    { this.userId = userId; }
    public void setStockId(Long stockId)                  { this.stockId = stockId; }
    public void setYear(int year)                         { this.year = year; }
    public void setMonth(int month)                       { this.month = month; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public void setConfirmedAmount(BigDecimal confirmedAmount) { this.confirmedAmount = confirmedAmount; }
    public void setExDividendDate(LocalDate exDividendDate) { this.exDividendDate = exDividendDate; }
    public void setPaymentDate(LocalDate paymentDate)     { this.paymentDate = paymentDate; }
    public void setStatus(String status)                  { this.status = status; }
}
