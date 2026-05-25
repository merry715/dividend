package com.example.dividend.service;

import com.example.dividend.dto.request.DividendConfirmRequest;
import com.example.dividend.dto.request.DividendGenerateRequest;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DividendService {

    private final DividendRepository dividendRepository;
    private final StockRepository    stockRepository;

    // ── [1] 예상 배당 자동 생성 ─────────────────────────────────

    @Transactional
    public List<Dividend> generateExpectedDividends(Long userId, Long stockId, Integer targetYear) {
        Stock stock = stockRepository.findByIdAndUser_Id(stockId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다"));

        if (stock.getQuantity() < 1) {
            return List.of(); // 보유 수량 0이면 생성 안 함
        }

        int year = (targetYear != null) ? targetYear : LocalDate.now().getYear();
        List<Integer> paymentMonths = resolvePaymentMonths(stock.getDividendCycle());

        Integer perShare = stock.getExpectedDividendPerShare();
        BigDecimal expectedAmount = (perShare != null && perShare > 0)
                ? BigDecimal.valueOf((long) perShare * stock.getQuantity())
                : BigDecimal.ZERO;

        List<Dividend> created = new ArrayList<>();
        for (int month : paymentMonths) {
            if (dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(userId, stockId, year, month)) {
                continue; // 중복 방지
            }
            Dividend d = new Dividend();
            d.setUserId(userId);
            d.setStockId(stockId);
            d.setYear(year);
            d.setMonth(month);
            d.setExpectedAmount(expectedAmount);
            d.setStatus("EXPECTED");
            created.add(dividendRepository.save(d));
        }
        return created;
    }

    // ── [2] 배당 확정 처리 (EXPECTED → CONFIRMED) ───────────────

    @Transactional
    public Dividend confirm(Long dividendId, Long userId, DividendConfirmRequest req) {
        Dividend dividend = dividendRepository.findByIdAndUserId(dividendId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "배당 정보를 찾을 수 없습니다: " + dividendId));

        if (!"EXPECTED".equals(dividend.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "이미 확정된 배당이거나 상태 전환이 불가능합니다");
        }

        dividend.setStatus("CONFIRMED");
        dividend.setConfirmedAmount(BigDecimal.valueOf(req.getConfirmedAmount()));
        dividend.setPaymentDate(req.getPaymentDate());
        return dividendRepository.save(dividend);
    }

    // ── [3] 월별 배당금 조회 ────────────────────────────────────

    public List<Map<String, Object>> getMonthly(Long userId, int year) {
        List<Dividend> dividends = dividendRepository.findByUserIdAndYear(userId, year);

        long[] expected  = new long[13]; // index 1~12
        long[] confirmed = new long[13];

        for (Dividend d : dividends) {
            int m = d.getMonth();
            if (m < 1 || m > 12) continue;
            if ("EXPECTED".equals(d.getStatus()) && d.getExpectedAmount() != null) {
                expected[m] += d.getExpectedAmount().longValue();
            } else if ("CONFIRMED".equals(d.getStatus())) {
                if (d.getConfirmedAmount() != null) confirmed[m] += d.getConfirmedAmount().longValue();
                if (d.getExpectedAmount()  != null) expected[m]  += d.getExpectedAmount().longValue();
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month",           m);
            row.put("expectedAmount",  expected[m]);
            row.put("confirmedAmount", confirmed[m]);
            result.add(row);
        }
        return result;
    }

    // ── [4] 연간 예상 배당금 ────────────────────────────────────

    public Map<String, Object> getAnnual(Long userId, int year) {
        List<Dividend> dividends = dividendRepository.findByUserIdAndYear(userId, year);

        long total = dividends.stream()
                .mapToLong(d -> {
                    if ("CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null)
                        return d.getConfirmedAmount().longValue();
                    if ("EXPECTED".equals(d.getStatus()) && d.getExpectedAmount() != null)
                        return d.getExpectedAmount().longValue();
                    return 0L;
                })
                .sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year",                 year);
        result.put("totalExpectedAmount",  total);
        return result;
    }

    // ── [5] 누적 배당금 조회 ────────────────────────────────────

    public Map<String, Object> getCumulative(Long userId) {
        List<Object[]> rows = dividendRepository.findCumulativeAggregation(userId);

        long totalConfirmed = 0L, totalExpected = 0L;
        if (!rows.isEmpty() && rows.get(0) != null) {
            Object[] row = rows.get(0);
            totalConfirmed = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            totalExpected  = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalConfirmedAmount", totalConfirmed);
        result.put("totalExpectedAmount",  totalExpected);
        return result;
    }

    // ── [6] 연도별 배당금 조회 ──────────────────────────────────

    public List<Map<String, Object>> getYearly(Long userId) {
        List<Object[]> rows = dividendRepository.findYearlyAggregation(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("year",            ((Number) row[0]).intValue());
            item.put("confirmedAmount", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            item.put("expectedAmount",  row[2] != null ? ((Number) row[2]).longValue() : 0L);
            result.add(item);
        }
        return result;
    }

    // ── 내부 유틸 ────────────────────────────────────────────────

    private List<Integer> resolvePaymentMonths(String dividendCycle) {
        if (dividendCycle == null) return List.of(12);
        return switch (dividendCycle.toUpperCase()) {
            case "MONTHLY"   -> List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            case "QUARTERLY" -> List.of(3, 6, 9, 12);
            default          -> List.of(12); // ANNUAL
        };
    }
}
