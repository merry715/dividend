package com.example.dividend.service;

import com.example.dividend.dto.request.TransactionCreateRequest;
import com.example.dividend.dto.request.TransactionUpdateRequest;
import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAll(Long userId, Integer year, String type) {
        if (year != null && type != null) {
            return transactionRepository.findByUserIdAndYearAndType(userId, year, type.toUpperCase());
        }
        if (year != null) {
            return transactionRepository.findByUserIdAndYear(userId, year);
        }
        if (type != null) {
            return transactionRepository.findByUserIdAndType(userId, type.toUpperCase());
        }
        return transactionRepository.findByUserId(userId);
    }

    public Transaction add(Long userId, TransactionCreateRequest req) {
        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setStockId(req.getStockId());
        t.setType(req.getType());
        t.setQuantity(req.getQuantity());
        t.setPrice(req.getPrice());
        t.setDate(req.getDate());
        t.setBrokerFee(req.getBrokerFee());
        t.setTransactionTax(req.getTransactionTax());
        return transactionRepository.save(t);
    }

    public Transaction update(Long id, TransactionUpdateRequest req) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("거래를 찾을 수 없습니다: " + id));

        if (req.getType()           != null) t.setType(req.getType());
        if (req.getQuantity()       != null) t.setQuantity(req.getQuantity());
        if (req.getPrice()          != null) t.setPrice(req.getPrice());
        if (req.getDate()           != null) t.setDate(req.getDate());
        if (req.getBrokerFee()      != null) t.setBrokerFee(req.getBrokerFee());
        if (req.getTransactionTax() != null) t.setTransactionTax(req.getTransactionTax());

        return transactionRepository.save(t);
    }

    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new NoSuchElementException("거래를 찾을 수 없습니다: " + id);
        }
        transactionRepository.deleteById(id);
    }

    public List<Transaction> getByStockId(Long userId, Long stockId) {
        return transactionRepository.findByUserIdAndStockId(userId, stockId);
    }

    // 평균 단가 = (매수금액 합계 + 위탁수수료 합계) / 총 매수 수량
    public Map<String, Object> getStockHolding(Long userId, Long stockId) {
        List<Transaction> all = transactionRepository.findByUserIdAndStockId(userId, stockId);

        int totalBuyQty = 0, totalSellQty = 0;
        long totalBuyCost = 0;
        long totalBrokerFee = 0, totalTransactionTax = 0;

        for (Transaction t : all) {
            totalBrokerFee += t.getBrokerFee();
            totalTransactionTax += t.getTransactionTax();
            if ("BUY".equals(t.getType())) {
                totalBuyQty += t.getQuantity();
                totalBuyCost += (long) t.getQuantity() * t.getPrice() + t.getBrokerFee();
            } else {
                totalSellQty += t.getQuantity();
            }
        }

        int netQuantity = totalBuyQty - totalSellQty;
        long averagePrice = totalBuyQty > 0 ? totalBuyCost / totalBuyQty : 0;
        long totalInvestment = netQuantity > 0 ? netQuantity * averagePrice : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stockId", stockId);
        result.put("netQuantity", netQuantity);
        result.put("averagePrice", averagePrice);
        result.put("totalBuyQty", totalBuyQty);
        result.put("totalSellQty", totalSellQty);
        result.put("totalInvestment", totalInvestment);
        result.put("totalBrokerFee", totalBrokerFee);
        result.put("totalTransactionTax", totalTransactionTax);
        return result;
    }

    public List<Map<String, Object>> getAllHoldings(Long userId) {
        Map<Long, int[]> qtyMap = new LinkedHashMap<>();
        Map<Long, Long>  costMap = new LinkedHashMap<>();

        for (Transaction t : transactionRepository.findByUserId(userId)) {
            Long stockId = t.getStockId();
            qtyMap.putIfAbsent(stockId, new int[]{0, 0});
            costMap.putIfAbsent(stockId, 0L);

            if ("BUY".equals(t.getType())) {
                qtyMap.get(stockId)[0] += t.getQuantity();
                costMap.merge(stockId, (long) t.getQuantity() * t.getPrice() + t.getBrokerFee(), Long::sum);
            } else {
                qtyMap.get(stockId)[1] += t.getQuantity();
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, int[]> entry : qtyMap.entrySet()) {
            Long stockId = entry.getKey();
            int buyQty  = entry.getValue()[0];
            int sellQty = entry.getValue()[1];
            int netQty  = buyQty - sellQty;
            long buyCost = costMap.getOrDefault(stockId, 0L);
            long avgPrice = buyQty > 0 ? buyCost / buyQty : 0;

            Map<String, Object> holding = new LinkedHashMap<>();
            holding.put("stockId", stockId);
            holding.put("netQuantity", netQty);
            holding.put("averagePrice", avgPrice);
            holding.put("totalInvestment", netQty > 0 ? netQty * avgPrice : 0);
            result.add(holding);
        }
        return result;
    }

    public Map<String, Object> getSummary(Long userId) {
        List<Transaction> all = transactionRepository.findByUserId(userId);

        long totalBuyAmount = 0, totalSellAmount = 0, totalBrokerFee = 0, totalTransactionTax = 0;
        for (Transaction t : all) {
            long amount = (long) t.getQuantity() * t.getPrice();
            if ("BUY".equals(t.getType())) totalBuyAmount  += amount;
            else                            totalSellAmount += amount;
            totalBrokerFee      += t.getBrokerFee();
            totalTransactionTax += t.getTransactionTax();
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTransactions",    all.size());
        summary.put("totalBuyAmount",       totalBuyAmount);
        summary.put("totalSellAmount",      totalSellAmount);
        summary.put("netInvestment",        totalBuyAmount - totalSellAmount);
        summary.put("totalBrokerFee",       totalBrokerFee);
        summary.put("totalTransactionTax",  totalTransactionTax);
        summary.put("totalCost",            totalBuyAmount + totalBrokerFee + totalTransactionTax);
        return summary;
    }

    public Map<String, Object> getMonthlyChart(Long userId, int year) {
        List<Transaction> filtered = transactionRepository.findByUserIdAndYear(userId, year);

        Map<Integer, long[]> monthMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) monthMap.put(i, new long[]{0L, 0L});

        for (Transaction t : filtered) {
            int month  = t.getDate().getMonthValue();
            long amount = (long) t.getQuantity() * t.getPrice();
            if ("BUY".equals(t.getType())) monthMap.get(month)[0] += amount;
            else                            monthMap.get(month)[1] += amount;
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : monthMap.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month",      e.getKey());
            row.put("buyAmount",  e.getValue()[0]);
            row.put("sellAmount", e.getValue()[1]);
            data.add(row);
        }

        return Map.of("year", year, "data", data);
    }
}
