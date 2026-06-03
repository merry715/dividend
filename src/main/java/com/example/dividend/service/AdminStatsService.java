package com.example.dividend.service;

import com.example.dividend.entity.StockSector;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import com.example.dividend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final DividendRepository dividendRepository;

    @Cacheable(value = "adminStats", key = "#root.methodName")
    public Map<String, Object> getUserStats() {
        long total = userRepository.count();

        List<Object[]> rows = userRepository.findMonthlySignupTrend();
        List<Map<String, Object>> monthly = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", r[0]);
            m.put("count", ((Number) r[1]).longValue());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", total);
        result.put("monthlyTrend", monthly);
        return result;
    }

    @Cacheable(value = "adminStats", key = "#root.methodName")
    public Map<String, Object> getActiveUsers() {
        long activeCount = userRepository.countActiveUsers();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeUsers", activeCount);
        return result;
    }

    @Cacheable(value = "adminStats", key = "#root.methodName")
    public List<Map<String, Object>> getTop10Stocks() {
        List<Object[]> rows = stockRepository.findTop10Stocks();
        return rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stockCode", r[0]);
            m.put("stockName", r[1]);
            m.put("count", ((Number) r[2]).longValue());
            return m;
        }).toList();
    }

    @Cacheable(value = "adminStats", key = "#root.methodName")
    public List<Map<String, Object>> getSectorDistribution() {
        List<Object[]> rows = stockRepository.findSectorDistribution();

        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        return rows.stream().map(r -> {
            String sectorName = (String) r[0];
            long count = ((Number) r[1]).longValue();

            String label = sectorName;
            try {
                label = StockSector.valueOf(sectorName).getLabel();
            } catch (IllegalArgumentException ignored) {}

            double weight = total > 0
                    ? BigDecimal.valueOf(count * 100.0 / total)
                            .setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sector", sectorName);
            m.put("label", label);
            m.put("count", count);
            m.put("weight", weight);
            return m;
        }).toList();
    }

    @Cacheable(value = "adminStats", key = "#root.methodName")
    public Map<String, Object> getDividendAverages() {
        List<Object[]> bySector = dividendRepository.findAvgDividendBySector();
        List<Object[]> byStock = dividendRepository.findAvgDividendByStock();

        List<Map<String, Object>> sectorList = bySector.stream().map(r -> {
            String sectorName = (String) r[0];
            String label = sectorName;
            try {
                label = StockSector.valueOf(sectorName).getLabel();
            } catch (IllegalArgumentException ignored) {}

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sector", sectorName);
            m.put("label", label);
            m.put("avgAmount", r[1] != null ? ((Number) r[1]).longValue() : 0L);
            return m;
        }).toList();

        List<Map<String, Object>> stockList = byStock.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stockCode", r[0]);
            m.put("stockName", r[1]);
            m.put("avgAmount", r[2] != null ? ((Number) r[2]).longValue() : 0L);
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bySector", sectorList);
        result.put("byStock", stockList);
        return result;
    }
}
