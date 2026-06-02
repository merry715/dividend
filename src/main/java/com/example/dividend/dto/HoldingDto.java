package com.example.dividend.dto;

public class HoldingDto {

    private String stockName;
    private int quantity;
    private long totalInvestment;
    private int expectedDividend;
    private long averagePrice;

    public HoldingDto(String stockName, int quantity, long totalInvestment, int expectedDividend, long averagePrice) {
        this.stockName = stockName;
        this.quantity = quantity;
        this.totalInvestment = totalInvestment;
        this.expectedDividend = expectedDividend;
        this.averagePrice = averagePrice;
    }

    public String getStockName() {
        return stockName;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTotalInvestment() {
        return totalInvestment;
    }

    public int getExpectedDividend() {
        return expectedDividend;
    }

    public long getAveragePrice() {
        return averagePrice;
    }
}