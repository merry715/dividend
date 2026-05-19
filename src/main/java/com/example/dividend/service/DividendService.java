package com.example.dividend.service;

import com.example.dividend.entity.Dividend;
import com.example.dividend.repository.DividendRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class DividendService {

    private final DividendRepository dividendRepository;

    public DividendService(DividendRepository dividendRepository) {
        this.dividendRepository = dividendRepository;
    }

    public List<Dividend> getAll() {
        return dividendRepository.findAll();
    }

    public List<Dividend> generate(Map<String, Object> req) {
        // TODO: 보유 종목 기반 예상 배당 자동 생성
        // 1. req에서 year 추출
        // 2. HoldingStock 조회
        // 3. 종목별 직전 연도 배당 이력 기반으로 expectedDividend 추정
        // 4. Dividend 레코드 생성 (status = EXPECTED) 후 저장
        return List.of();
    }

    public Dividend confirm(Long id, Map<String, Object> req) {
        Dividend dividend = dividendRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("배당 정보를 찾을 수 없습니다: " + id));

        dividend.setStatus("CONFIRMED");
        if (req.containsKey("confirmedDividend"))
            dividend.setConfirmedDividend(Integer.parseInt(req.get("confirmedDividend").toString()));
        if (req.containsKey("paymentDate"))
            dividend.setPaymentDate(LocalDate.parse(req.get("paymentDate").toString()));

        return dividendRepository.save(dividend);
    }

    public List<Map<String, Object>> getMonthly() {
        Map<Integer, int[]> monthMap = new TreeMap<>();
        for (int i = 1; i <= 12; i++) monthMap.put(i, new int[]{0, 0}); // [expected, confirmed]

        for (Dividend d : dividendRepository.findAll()) {
            int month = d.getPaymentMonth();
            if (monthMap.containsKey(month)) {
                monthMap.get(month)[0] += d.getExpectedDividend();
                monthMap.get(month)[1] += d.getConfirmedDividend();
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : monthMap.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month",     e.getKey());
            row.put("expected",  e.getValue()[0]);
            row.put("confirmed", e.getValue()[1]);
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> getAnnual() {
        int currentYear = LocalDate.now().getYear();
        List<Dividend> dividends = dividendRepository.findByYear(currentYear);

        int totalExpected  = dividends.stream().mapToInt(Dividend::getExpectedDividend).sum();
        int totalConfirmed = dividends.stream().mapToInt(Dividend::getConfirmedDividend).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year",           currentYear);
        result.put("totalExpected",  totalExpected);
        result.put("totalConfirmed", totalConfirmed);
        return result;
    }

    public List<Map<String, Object>> getYearly() {
        Map<Integer, int[]> yearMap = new TreeMap<>();
        for (Dividend d : dividendRepository.findAll()) {
            yearMap.computeIfAbsent(d.getYear(), k -> new int[]{0, 0});
            yearMap.get(d.getYear())[0] += d.getExpectedDividend();
            yearMap.get(d.getYear())[1] += d.getConfirmedDividend();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : yearMap.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("year",          e.getKey());
            row.put("totalExpected", e.getValue()[0]);
            row.put("totalConfirmed", e.getValue()[1]);
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getCumulative() {
        List<Map<String, Object>> yearly = getYearly();

        List<Map<String, Object>> result = new ArrayList<>();
        int cumulative = 0;
        for (Map<String, Object> row : yearly) {
            cumulative += (int) row.get("totalConfirmed");
            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("cumulative", cumulative);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getByStock() {
        Map<Long, int[]> stockMap = new LinkedHashMap<>();
        for (Dividend d : dividendRepository.findAll()) {
            stockMap.computeIfAbsent(d.getStockId(), k -> new int[]{0, 0});
            stockMap.get(d.getStockId())[0] += d.getExpectedDividend();
            stockMap.get(d.getStockId())[1] += d.getConfirmedDividend();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : stockMap.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stockId",       e.getKey());
            row.put("totalExpected", e.getValue()[0]);
            row.put("totalConfirmed", e.getValue()[1]);
            result.add(row);
        }
        return result;
    }
}
