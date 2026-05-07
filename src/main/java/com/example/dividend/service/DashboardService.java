package com.example.dividend.service;

import com.example.dividend.dto.HoldingDto;
import com.example.dividend.dto.MonthlyDividendDto;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final DividendRepository dividendRepository;

    public DashboardService(TransactionRepository transactionRepository,
                            DividendRepository dividendRepository) {
        this.transactionRepository = transactionRepository;
        this.dividendRepository = dividendRepository;
    }

    // 🔥 보유 종목 + 배당 + 평균단가 계산
    public List<HoldingDto> getHoldings() {

        List<Transaction> transactions = transactionRepository.findAll();
        List<Dividend> dividends = dividendRepository.findAll();

        Map<String, Integer> quantityMap = new HashMap<>();
        Map<String, Integer> investmentMap = new HashMap<>();
        Map<String, Integer> dividendMap = new HashMap<>();

        // 1️⃣ 보유 수량 / 투자금 계산
        for (Transaction t : transactions) {

            String name = t.getStock().getStockName();
            int quantity = t.getQuantity();
            int amount = t.getQuantity() * t.getPrice();

            if (t.getTradeType().equals("매수")) {
                quantityMap.put(name, quantityMap.getOrDefault(name, 0) + quantity);
                investmentMap.put(name, investmentMap.getOrDefault(name, 0) + amount);
            } else {
                quantityMap.put(name, quantityMap.getOrDefault(name, 0) - quantity);
                investmentMap.put(name, investmentMap.getOrDefault(name, 0) - amount);
            }
        }

        // 2️⃣ 연간 주당 배당금 합계
        for (Dividend d : dividends) {
            String name = d.getStock().getStockName();
            dividendMap.put(
                    name,
                    dividendMap.getOrDefault(name, 0) + d.getDividendPerShare()
            );
        }

        // 3️⃣ 결과 생성
        List<HoldingDto> result = new ArrayList<>();

        for (String name : quantityMap.keySet()) {

            int quantity = quantityMap.get(name);
            int totalInvestment = investmentMap.get(name);
            int annualDividendPerShare = dividendMap.getOrDefault(name, 0);

            int expectedDividend = quantity * annualDividendPerShare;

            // 🔥 평균 단가 계산
            int averagePrice = 0;
            if (quantity > 0) {
                averagePrice = totalInvestment / quantity;
            }

            result.add(new HoldingDto(
                    name,
                    quantity,
                    totalInvestment,
                    expectedDividend,
                    averagePrice
            ));
        }

        return result;
    }

    // 🔥 월별 배당금 계산
    public List<MonthlyDividendDto> getMonthlyDividends() {

        List<Transaction> transactions = transactionRepository.findAll();
        List<Dividend> dividends = dividendRepository.findAll();

        Map<String, Integer> quantityMap = new HashMap<>();

        // 보유 수량 계산
        for (Transaction t : transactions) {
            String name = t.getStock().getStockName();
            int quantity = t.getQuantity();

            if (t.getTradeType().equals("매수")) {
                quantityMap.put(name, quantityMap.getOrDefault(name, 0) + quantity);
            } else {
                quantityMap.put(name, quantityMap.getOrDefault(name, 0) - quantity);
            }
        }

        // 월별 초기화
        Map<Integer, Integer> monthlyMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyMap.put(i, 0);
        }

        // 월별 배당 계산
        for (Dividend d : dividends) {
            String name = d.getStock().getStockName();
            int quantity = quantityMap.getOrDefault(name, 0);
            int month = d.getPaymentMonth();

            int amount = quantity * d.getDividendPerShare();

            monthlyMap.put(month, monthlyMap.get(month) + amount);
        }

        List<MonthlyDividendDto> result = new ArrayList<>();

        for (int month : monthlyMap.keySet()) {
            result.add(new MonthlyDividendDto(month, monthlyMap.get(month)));
        }

        return result;
    }
}