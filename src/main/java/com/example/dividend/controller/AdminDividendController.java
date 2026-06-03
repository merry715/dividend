package com.example.dividend.controller;

import com.example.dividend.client.DataGoKrDividendClient;
import com.example.dividend.client.DataGoKrDividendInfo;
import com.example.dividend.client.DartNaverClient;
import com.example.dividend.dto.ApiResponse;
import com.example.dividend.entity.Stock;
import com.example.dividend.repository.StockRepository;
import com.example.dividend.service.DividendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자용 배당 수동 갱신 API.
 * ROLE_ADMIN만 호출 가능 (SecurityConfig에서 /api/admin/** 에 hasRole(ADMIN) 적용됨).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dividends")
@RequiredArgsConstructor
public class AdminDividendController {

    private final StockRepository        stockRepository;
    private final DataGoKrDividendClient dataGoKrClient;
    private final DividendService        dividendService;
    private final DartNaverClient        dartClient;

    /**
     * 특정 종목 배당 데이터 수동 갱신.
     * POST /api/admin/dividends/refresh?stockCode=005930
     */
    @PostMapping("/refresh")
    public ApiResponse<String> refresh(@RequestParam String stockCode) {
        int year = LocalDate.now().getYear();
        List<Stock> stocks = stockRepository.findAll().stream()
                .filter(s -> stockCode.equals(s.getStockCode()))
                .collect(Collectors.toList());

        if (stocks.isEmpty()) {
            return ApiResponse.ok("해당 종목 없음: " + stockCode);
        }

        int updated = 0;
        for (Stock stock : stocks) {
            try {
                List<DataGoKrDividendInfo> apiRecords =
                        dataGoKrClient.fetchAllDividendRecords(stockCode, stock.getStockName(), year - 1);
                List<DataGoKrDividendInfo> validRecords = apiRecords.stream()
                        .filter(r -> r.getPayDate() != null)
                        .sorted(Comparator.comparing(r -> r.getPayDate().getMonthValue()))
                        .collect(Collectors.toList());

                if (validRecords.isEmpty()) {
                    log.info("[관리자 갱신] API 데이터 없음 [stockCode={}]", stockCode);
                    continue;
                }

                String cycle = dividendService.detectCycle(validRecords.size());
                dividendService.generateFromApiData(stock.getUser().getId(), stock.getId(),
                        year, validRecords, cycle);
                updated++;
                log.info("[관리자 갱신] 완료 [stockCode={}, userId={}]", stockCode, stock.getUser().getId());
            } catch (Exception e) {
                log.warn("[관리자 갱신] 실패 [stockCode={}, userId={}]: {}",
                        stockCode, stock.getUser().getId(), e.getMessage());
            }
        }

        return ApiResponse.ok(String.format("[%s] %d개 사용자 배당 데이터 갱신 완료", stockCode, updated));
    }

    /**
     * 전체 종목의 배당주기(dividendCycle)를 DART에서 재감지해 DB 업데이트.
     * 이전 버그로 잘못 저장된 cycle 값을 일괄 수정.
     * POST /api/admin/dividends/fix-cycles
     */
    @PostMapping("/fix-cycles")
    public ApiResponse<String> fixAllCycles() {
        int year = LocalDate.now().getYear();
        List<Stock> all = stockRepository.findAll().stream()
                .filter(s -> s.getDeletedAt() == null)
                .collect(Collectors.toList());

        int fixed = 0, skipped = 0, fail = 0;

        for (Stock stock : all) {
            try {
                // DART에서 cycle 재감지 (year-1, year-2 순으로 시도)
                String newCycle = dartClient.detectDividendCycle(stock.getStockCode(), year - 1);

                if (newCycle == null) {
                    log.info("[cycle 갱신] DART 감지 실패 — 건너뜀 [stockCode={}, 현재={}]",
                            stock.getStockCode(), stock.getDividendCycle());
                    skipped++;
                    continue;
                }

                String oldCycle = stock.getDividendCycle();
                if (newCycle.equals(oldCycle)) {
                    log.info("[cycle 갱신] 변경 없음 [stockCode={}, cycle={}]",
                            stock.getStockCode(), newCycle);
                    skipped++;
                    continue;
                }

                stock.setDividendCycle(newCycle);
                stockRepository.save(stock);
                log.info("[cycle 갱신] 완료 [stockCode={}, {} → {}]",
                        stock.getStockCode(), oldCycle, newCycle);
                fixed++;

                Thread.sleep(300); // DART API 부하 방지
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[cycle 갱신] 실패 [stockCode={}]: {}", stock.getStockCode(), e.getMessage());
                fail++;
            }
        }

        String msg = String.format("배당주기 갱신 완료 — 수정: %d건, 변경없음: %d건, 실패: %d건",
                fixed, skipped, fail);
        log.info("[cycle 갱신] {}", msg);
        return ApiResponse.ok(msg);
    }
}
