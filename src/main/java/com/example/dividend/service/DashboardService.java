package com.example.dividend.service;

import com.example.dividend.dto.HoldingDto;
import com.example.dividend.dto.MonthlyDividendDto;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import com.example.dividend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final DividendRepository dividendRepository;
    private final StockRepository stockRepository;

    public DashboardService(TransactionRepository transactionRepository,
                            DividendRepository dividendRepository,
                            StockRepository stockRepository) {
        this.transactionRepository = transactionRepository;
        this.dividendRepository = dividendRepository;
        this.stockRepository = stockRepository;
    }

    public List<HoldingDto> getHoldings() {
        Map<Long, Integer> quantityMap = new HashMap<>();
        Map<Long, Integer> investmentMap = new HashMap<>();

        for (Transaction t : transactionRepository.findAll()) {
            Long stockId = t.getStockId();
            int amount = t.getQuantity() * t.getPrice();

            if ("BUY".equals(t.getType())) {
                quantityMap.merge(stockId, t.getQuantity(), Integer::sum);
                investmentMap.merge(stockId, amount, Integer::sum);
            } else {
                quantityMap.merge(stockId, -t.getQuantity(), Integer::sum);
                investmentMap.merge(stockId, -amount, Integer::sum);
            }
        }

        Map<Long, Integer> dividendMap = new HashMap<>();
        for (Dividend d : dividendRepository.findAll()) {
            dividendMap.merge(d.getStockId(), d.getExpectedDividend(), Integer::sum);
        }

        List<HoldingDto> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantityMap.entrySet()) {
            Long stockId = entry.getKey();
            int quantity = entry.getValue();
            int totalInvestment = investmentMap.getOrDefault(stockId, 0);
            int expectedDividend = dividendMap.getOrDefault(stockId, 0) * quantity;
            int averagePrice = quantity > 0 ? totalInvestment / quantity : 0;

            String stockName = stockRepository.findById(stockId)
                    .map(Stock::getStockName)
                    .orElse("Unknown");

            result.add(new HoldingDto(stockName, quantity, totalInvestment, expectedDividend, averagePrice));
        }

        return result;
    }

    public List<MonthlyDividendDto> getMonthlyDividends() {
        Map<Long, Integer> quantityMap = new HashMap<>();
        for (Transaction t : transactionRepository.findAll()) {
            if ("BUY".equals(t.getType())) {
                quantityMap.merge(t.getStockId(), t.getQuantity(), Integer::sum);
            } else {
                quantityMap.merge(t.getStockId(), -t.getQuantity(), Integer::sum);
            }
        }

        Map<Integer, Integer> monthlyMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) monthlyMap.put(i, 0);

        for (Dividend d : dividendRepository.findAll()) {
            int quantity = quantityMap.getOrDefault(d.getStockId(), 0);
            monthlyMap.merge(d.getPaymentMonth(), quantity * d.getExpectedDividend(), Integer::sum);
        }

        List<MonthlyDividendDto> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : monthlyMap.entrySet()) {
            result.add(new MonthlyDividendDto(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
