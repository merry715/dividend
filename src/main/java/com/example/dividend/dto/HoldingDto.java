package com.example.dividend.dto;

public class HoldingDto {

    private String stockName;
    private int quantity;
    private int totalInvestment;
    private int expectedDividend;
    private int averagePrice;

    public HoldingDto(String stockName, int quantity, int totalInvestment, int expectedDividend, int averagePrice) {
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

    public int getTotalInvestment() {
        return totalInvestment;
    }

    public int getExpectedDividend() {
        return expectedDividend;
    }

    public int getAveragePrice() {
        return averagePrice;
    }
}