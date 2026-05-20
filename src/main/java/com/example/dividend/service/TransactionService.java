package com.example.dividend.service;

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

    // 연도·유형 필터 조회 (DB 레벨 쿼리)
    public List<Transaction> getAll(Integer year, String type) {
        if (year != null && type != null) {
            return transactionRepository.findByYearAndType(year, type.toUpperCase());
        }
        if (year != null) {
            return transactionRepository.findByYear(year);
        }
        if (type != null) {
            return transactionRepository.findByType(type.toUpperCase());
        }
        return transactionRepository.findAll();
    }

    public Transaction add(Map<String, Object> req) {
        Transaction t = new Transaction();
        t.setStockId(Long.valueOf(req.get("stockId").toString()));
        t.setType(req.get("type").toString().toUpperCase());
        t.setQuantity(Integer.parseInt(req.get("quantity").toString()));
        t.setPrice(Integer.parseInt(req.get("price").toString()));
        t.setDate(LocalDate.parse(req.get("date").toString()));
        t.setBrokerFee(Integer.parseInt(req.getOrDefault("brokerFee", "0").toString()));
        t.setTransactionTax(Integer.parseInt(req.getOrDefault("transactionTax", "0").toString()));
        return transactionRepository.save(t);
    }

    public Transaction update(Long id, Map<String, Object> req) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("거래를 찾을 수 없습니다: " + id));

        if (req.containsKey("stockId"))        t.setStockId(Long.valueOf(req.get("stockId").toString()));
        if (req.containsKey("type"))           t.setType(req.get("type").toString().toUpperCase());
        if (req.containsKey("quantity"))       t.setQuantity(Integer.parseInt(req.get("quantity").toString()));
        if (req.containsKey("price"))          t.setPrice(Integer.parseInt(req.get("price").toString()));
        if (req.containsKey("date"))           t.setDate(LocalDate.parse(req.get("date").toString()));
        if (req.containsKey("brokerFee"))      t.setBrokerFee(Integer.parseInt(req.get("brokerFee").toString()));
        if (req.containsKey("transactionTax")) t.setTransactionTax(Integer.parseInt(req.get("transactionTax").toString()));

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

    // 특정 종목의 보유 수량 및 평균 단가 계산
    // 평균 단가 = (매수금액 합계 + 위탁수수료 합계) / 총 매수 수량
    public Map<String, Object> getStockHolding(Long stockId) {
        List<Transaction> all = transactionRepository.findByStockId(stockId);

        int totalBuyQty = 0, totalSellQty = 0;
        long totalBuyCost = 0;  // 매수 원가 (수수료 포함)
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

    // 전체 종목의 보유 현황 집계
    public List<Map<String, Object>> getAllHoldings() {
        Map<Long, int[]> qtyMap = new LinkedHashMap<>();   // stockId -> [buyQty, sellQty]
        Map<Long, Long>  costMap = new LinkedHashMap<>();  // stockId -> buyCost (수수료 포함)

        for (Transaction t : transactionRepository.findAll()) {
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

    // 전체 거래 요약 (수수료·세금 포함)
    public Map<String, Object> getSummary() {
        List<Transaction> all = transactionRepository.findAll();

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

    public Map<String, Object> getMonthlyChart(int year) {
        List<Transaction> filtered = transactionRepository.findByYear(year);

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
