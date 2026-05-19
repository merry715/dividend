package com.example.dividend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long stockId;

    @Column(nullable = false)
    private String type;   // BUY / SELL

    private int quantity;
    private int price;
    private LocalDate date;
    private int brokerFee;
    private int transactionTax;

    public Long getId() {
        return id;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getBrokerFee() {
        return brokerFee;
    }

    public void setBrokerFee(int brokerFee) {
        this.brokerFee = brokerFee;
    }

    public int getTransactionTax() {
        return transactionTax;
    }

    public void setTransactionTax(int transactionTax) {
        this.transactionTax = transactionTax;
    }
}
