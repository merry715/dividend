package com.example.dividend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Dividend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long stockId;

    private int expectedDividend;
    private int confirmedDividend;
    private int paymentMonth;
    private LocalDate exDividendDate;
    private LocalDate paymentDate;

    @Column(nullable = false)
    private String status;   // EXPECTED / CONFIRMED

    private int year;

    public Long getId() {
        return id;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public int getExpectedDividend() {
        return expectedDividend;
    }

    public void setExpectedDividend(int expectedDividend) {
        this.expectedDividend = expectedDividend;
    }

    public int getConfirmedDividend() {
        return confirmedDividend;
    }

    public void setConfirmedDividend(int confirmedDividend) {
        this.confirmedDividend = confirmedDividend;
    }

    public int getPaymentMonth() {
        return paymentMonth;
    }

    public void setPaymentMonth(int paymentMonth) {
        this.paymentMonth = paymentMonth;
    }

    public LocalDate getExDividendDate() {
        return exDividendDate;
    }

    public void setExDividendDate(LocalDate exDividendDate) {
        this.exDividendDate = exDividendDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
