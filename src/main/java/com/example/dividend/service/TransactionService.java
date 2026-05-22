package com.example.dividend.service;

import com.example.dividend.dto.request.TransactionCreateRequest;
import com.example.dividend.dto.request.TransactionUpdateRequest;
import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAll(Integer year, String type) {
        return transactionRepository.findAll().stream()
                .filter(t -> year == null || (t.getDate() != null && t.getDate().getYear() == year))
                .filter(t -> type == null || type.equalsIgnoreCase(t.getType()))
                .toList();
    }

    public Transaction add(TransactionCreateRequest req) {
        Transaction t = new Transaction();
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

    public List<Transaction> getByStockId(Long stockId) {
        return transactionRepository.findByStockId(stockId);
    }

    public Map<String, Object> getSummary() {
        List<Transaction> all = transactionRepository.findAll();

        int totalBuyAmount = 0, totalSellAmount = 0, totalBrokerFee = 0, totalTransactionTax = 0;
        for (Transaction t : all) {
            int amount = t.getQuantity() * t.getPrice();
            if ("BUY".equals(t.getType())) totalBuyAmount += amount;
            else                            totalSellAmount += amount;
            totalBrokerFee     += t.getBrokerFee();
            totalTransactionTax += t.getTransactionTax();
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTransactions",  all.size());
        summary.put("totalBuyAmount",     totalBuyAmount);
        summary.put("totalSellAmount",    totalSellAmount);
        summary.put("netInvestment",      totalBuyAmount - totalSellAmount);
        summary.put("totalBrokerFee",     totalBrokerFee);
        summary.put("totalTransactionTax", totalTransactionTax);
        return summary;
    }

    public Map<String, Object> getMonthlyChart(int year) {
        List<Transaction> filtered = transactionRepository.findAll().stream()
                .filter(t -> t.getDate() != null && t.getDate().getYear() == year)
                .toList();

        Map<Integer, int[]> monthMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) monthMap.put(i, new int[]{0, 0}); // [buyAmount, sellAmount]

        for (Transaction t : filtered) {
            int month  = t.getDate().getMonthValue();
            int amount = t.getQuantity() * t.getPrice();
            if ("BUY".equals(t.getType())) monthMap.get(month)[0] += amount;
            else                            monthMap.get(month)[1] += amount;
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : monthMap.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month",      e.getKey());
            row.put("buyAmount",  e.getValue()[0]);
            row.put("sellAmount", e.getValue()[1]);
            data.add(row);
        }

        return Map.of("year", year, "data", data);
    }
}
