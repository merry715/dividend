package com.example.dividend.service;

import com.example.dividend.dto.request.TransactionCreateRequest;
import com.example.dividend.dto.request.TransactionUpdateRequest;
import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final DividendRepository dividendRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              DividendRepository dividendRepository) {
        this.transactionRepository = transactionRepository;
        this.dividendRepository = dividendRepository;
    }

    /**
     * 거래 변경 후 순보유수량이 0 이하가 된 종목의 EXPECTED 배당 row 정리.
     * 거래 삭제·전량 매도로 미보유가 되면 고아 EXPECTED 배당이 남는 것을 방지 (read-side 보유 게이트의 보완).
     * CONFIRMED(실수령 기록일 수 있음)는 절대 삭제하지 않음.
     */
    private void cleanupExpectedDividendsIfUnheld(Long userId, Long stockId) {
        if (userId == null || stockId == null) return;
        if (transactionRepository.calculateNetQuantity(stockId) <= 0) {
            dividendRepository.deleteExpectedByUserIdAndStockId(userId, stockId);
        }
    }


    public List<Transaction> getAll(Long userId, Integer year, String type) {
        if (year != null && type != null) {
            return transactionRepository.findActiveByUserIdAndYearAndType(userId, year, type.toUpperCase());
        }
        if (year != null) {
            return transactionRepository.findActiveByUserIdAndYear(userId, year);
        }
        if (type != null) {
            return transactionRepository.findActiveByUserIdAndType(userId, type.toUpperCase());
        }
        return transactionRepository.findActiveByUserId(userId);
    }

    @Transactional
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
        Transaction saved = transactionRepository.save(t);
        // 전량 매도 등으로 순보유가 0이 되면 EXPECTED 배당 정리
        cleanupExpectedDividendsIfUnheld(userId, saved.getStockId());
        return saved;
    }

    @Transactional

    public Transaction update(Long id, TransactionUpdateRequest req) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("거래를 찾을 수 없습니다: " + id));

        if (req.getType()           != null) t.setType(req.getType());
        if (req.getQuantity()       != null) t.setQuantity(req.getQuantity());
        if (req.getPrice()          != null) t.setPrice(req.getPrice());
        if (req.getDate()           != null) t.setDate(req.getDate());
        if (req.getBrokerFee()      != null) t.setBrokerFee(req.getBrokerFee());
        if (req.getTransactionTax() != null) t.setTransactionTax(req.getTransactionTax());

        Transaction saved = transactionRepository.save(t);
        // 수량·유형 수정으로 순보유가 0이 되면 EXPECTED 배당 정리
        cleanupExpectedDividendsIfUnheld(saved.getUserId(), saved.getStockId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("거래를 찾을 수 없습니다: " + id));
        Long userId = t.getUserId();
        Long stockId = t.getStockId();
        transactionRepository.deleteById(id);
        // 삭제로 순보유가 0이 되면 EXPECTED 배당 정리 (CONFIRMED 보존)
        cleanupExpectedDividendsIfUnheld(userId, stockId);
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

        for (Transaction t : transactionRepository.findActiveByUserId(userId)) {
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
        List<Transaction> all = transactionRepository.findActiveByUserId(userId);

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
        List<Transaction> filtered = transactionRepository.findActiveByUserIdAndYear(userId, year);

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
