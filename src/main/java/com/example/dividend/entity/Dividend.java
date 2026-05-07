package com.example.dividend.entity;

import jakarta.persistence.*;

@Entity
public class Dividend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int dividendPerShare; // 1주당 배당금
    private int paymentMonth;     // 지급월: 1~12
    private String status;        // 예상 / 확정

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock stock;

    public Long getId() {
        return id;
    }

    public int getDividendPerShare() {
        return dividendPerShare;
    }

    public void setDividendPerShare(int dividendPerShare) {
        this.dividendPerShare = dividendPerShare;
    }

    public int getPaymentMonth() {
        return paymentMonth;
    }

    public void setPaymentMonth(int paymentMonth) {
        this.paymentMonth = paymentMonth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }
}