package com.example.dividend.dto;

public class MonthlyDividendDto {

    private int month;
    private int amount;

    public MonthlyDividendDto(int month, int amount) {
        this.month = month;
        this.amount = amount;
    }

    public int getMonth() {
        return month;
    }

    public int getAmount() {
        return amount;
    }
}