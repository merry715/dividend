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

    public List<HoldingDto> getHoldings(Long userId) {
        Map<Long, Integer> quantityMap = new HashMap<>();
        Map<Long, Long> investmentMap = new HashMap<>();

        for (Transaction t : transactionRepository.findActiveByUserId(userId)) {
            Long stockId = t.getStockId();
            long amount = (long) t.getQuantity() * t.getPrice();

            if ("BUY".equals(t.getType())) {
                quantityMap.merge(stockId, t.getQuantity(), Integer::sum);
                investmentMap.merge(stockId, amount, Long::sum);
            } else {
                quantityMap.merge(stockId, -t.getQuantity(), Integer::sum);
                investmentMap.merge(stockId, -amount, Long::sum);
            }
        }

        int currentYear = java.time.LocalDate.now().getYear();
        Map<Long, Integer> dividendMap = new HashMap<>();
        for (Dividend d : dividendRepository.findByUserIdAndYear(userId, currentYear)) {
            int amt = d.getExpectedAmount() != null ? d.getExpectedAmount().intValue() : 0;
            dividendMap.merge(d.getStockId(), amt, Integer::sum);
        }

        List<HoldingDto> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantityMap.entrySet()) {
            Long stockId = entry.getKey();
            int quantity = entry.getValue();
            long totalInvestment = investmentMap.getOrDefault(stockId, 0L);
            int expectedDividend = dividendMap.getOrDefault(stockId, 0);
            long averagePrice = quantity > 0 ? totalInvestment / quantity : 0L;

            String stockName = stockRepository.findById(stockId)
                    .map(Stock::getStockName)
                    .orElse("Unknown");

            result.add(new HoldingDto(stockName, quantity, totalInvestment, expectedDividend, averagePrice));
        }

        return result;
    }

    public List<MonthlyDividendDto> getMonthlyDividends(Long userId) {
        int year = java.time.LocalDate.now().getYear();
        Map<Integer, Integer> monthlyMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) monthlyMap.put(i, 0);

        for (Dividend d : dividendRepository.findByUserIdAndYear(userId, year)) {
            int amt = d.getExpectedAmount() != null ? d.getExpectedAmount().intValue() : 0;
            monthlyMap.merge(d.getMonth(), amt, Integer::sum);
        }

        List<MonthlyDividendDto> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : monthlyMap.entrySet()) {
            result.add(new MonthlyDividendDto(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
