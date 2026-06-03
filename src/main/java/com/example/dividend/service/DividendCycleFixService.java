package com.example.dividend.service;

import com.example.dividend.client.DataGoKrDividendClient;
import com.example.dividend.client.DataGoKrDividendInfo;
import com.example.dividend.client.DartNaverClient;
import com.example.dividend.entity.Stock;
import com.example.dividend.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 앱 시작 시 전체 종목의 dividendCycle 및 배당지급일을 공공데이터 API 실데이터로 보정.
 *
 * 처리 순서 (종목당):
 *   1. 공공데이터 API records 조회 (ISIN 기반)
 *   2. dvdnBasDt 월 간격으로 cycle 감지
 *   3. cycle DB 저장
 *   4. 올해 EXPECTED 배당 row 삭제 후 실데이터로 재생성 (지급일 최신화)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DividendCycleFixService {

    private final StockRepository        stockRepository;
    private final DataGoKrDividendClient dataGoKrClient;
    private final DividendService        dividendService;
    private final DartNaverClient        dartClient;

    @PostConstruct
    public void fixDividendCyclesOnStartup() {
        Thread t = new Thread(this::runFix, "dividend-cycle-fix");
        t.setDaemon(true);
        t.start();
    }

    @Transactional
    public void runFix() {
        int year = LocalDate.now().getYear();
        List<Stock> stocks = stockRepository.findAll().stream()
                .filter(s -> s.getDeletedAt() == null)
                .toList();

        if (stocks.isEmpty()) return;

        log.info("[배당 보정] 시작 — 대상 {}개 종목 (cycle + 지급일)", stocks.size());
        int cycleFixed = 0, dividendFixed = 0, skipped = 0;

        for (Stock stock : stocks) {
            try {
                Long userId = stock.getUser().getId();

                // 1. 공공데이터 API records 조회 (year-1, year-2 순)
                ApiResult result = fetchBestRecords(stock, year);

                // 2. cycle 감지 및 저장
                String detectedCycle = null;
                if (result != null) {
                    detectedCycle = dividendService.detectCycleFromBaseDtMonths(result.records);
                    if (detectedCycle == null)
                        detectedCycle = dividendService.detectCycle(result.records.size());
                }
                if (detectedCycle == null)
                    detectedCycle = dartClient.detectDividendCycle(stock.getStockCode(), year - 1);

                if (detectedCycle != null && !detectedCycle.equals(stock.getDividendCycle())) {
                    String old = stock.getDividendCycle();
                    stock.setDividendCycle(detectedCycle);
                    stockRepository.save(stock);
                    log.info("[배당 보정] cycle [{}] {} → {}", stock.getStockCode(), old, detectedCycle);
                    cycleFixed++;
                }

                // 3. 배당 row 재생성 (지급일 최신화)
                //    올해 cashDvdnPayDt 기준 records가 있으면 실데이터로 재생성
                List<DataGoKrDividendInfo> thisYearRecords = fetchThisYearPayDateRecords(stock, year);
                if (!thisYearRecords.isEmpty()) {
                    String cycle = stock.getDividendCycle() != null
                            ? stock.getDividendCycle()
                            : detectedCycle;
                    dividendService.generateFromApiData(userId, stock.getId(), year, thisYearRecords, cycle);
                    log.info("[배당 보정] 지급일 최신화 [{}] {}건", stock.getStockCode(), thisYearRecords.size());
                    dividendFixed++;
                } else if (result != null && !result.records.isEmpty()) {
                    // 올해 지급일 데이터 없음 → 작년 패턴으로 올해 연도 치환 재생성
                    String cycle = stock.getDividendCycle() != null
                            ? stock.getDividendCycle()
                            : detectedCycle;
                    List<DataGoKrDividendInfo> shifted = shiftToThisYear(result.records, year);
                    if (!shifted.isEmpty()) {
                        dividendService.generateFromApiData(userId, stock.getId(), year, shifted, cycle);
                        log.info("[배당 보정] 작년 패턴으로 지급일 추정 [{}] {}건",
                                stock.getStockCode(), shifted.size());
                        dividendFixed++;
                    }
                } else {
                    skipped++;
                }

                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[배당 보정] 실패 [{}]: {}", stock.getStockCode(), e.getMessage());
                skipped++;
            }
        }

        log.info("[배당 보정] 완료 — cycle수정: {}건, 지급일최신화: {}건, 건너뜀: {}건",
                cycleFixed, dividendFixed, skipped);
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private record ApiResult(List<DataGoKrDividendInfo> records, int year) {}

    /** year-1 → year-2 순으로 공공데이터 records 조회, 첫 번째 non-empty 반환 */
    private ApiResult fetchBestRecords(Stock stock, int year) {
        String apiName = dartClient.findCorpName(stock.getStockCode());
        if (apiName == null) apiName = stock.getStockName();

        for (int y = year - 1; y >= year - 2; y--) {
            try {
                List<DataGoKrDividendInfo> records =
                        dataGoKrClient.fetchAllDividendRecords(stock.getStockCode(), apiName, y);
                if (!records.isEmpty()) return new ApiResult(records, y);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 올해 cashDvdnPayDt 기준 records 조회.
     * API에서 올해 지급일이 있는 records만 필터링해 반환.
     */
    private List<DataGoKrDividendInfo> fetchThisYearPayDateRecords(Stock stock, int year) {
        String apiName = dartClient.findCorpName(stock.getStockCode());
        if (apiName == null) apiName = stock.getStockName();
        try {
            // dvdnBasDt 기준 year와 year-1 모두 조회해서 cashDvdnPayDt가 올해인 것 필터
            List<DataGoKrDividendInfo> all = dataGoKrClient.fetchAllDividendRecords(
                    stock.getStockCode(), apiName, year - 1);
            // cashDvdnPayDt가 올해인 것만
            List<DataGoKrDividendInfo> thisYear = all.stream()
                    .filter(r -> r.getPayDate() != null
                            && r.getPayDate().getYear() == year)
                    .sorted(Comparator.comparing(r -> r.getPayDate().getMonthValue()))
                    .collect(Collectors.toList());
            if (!thisYear.isEmpty()) return thisYear;

            // dvdnBasDt year도 조회
            List<DataGoKrDividendInfo> cur = dataGoKrClient.fetchAllDividendRecords(
                    stock.getStockCode(), apiName, year);
            return cur.stream()
                    .filter(r -> r.getPayDate() != null && r.getPayDate().getYear() == year)
                    .sorted(Comparator.comparing(r -> r.getPayDate().getMonthValue()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 작년 records의 지급일을 올해 연도로 치환 (패턴 기반 추정).
     * payDate가 있는 records만 사용.
     */
    private List<DataGoKrDividendInfo> shiftToThisYear(
            List<DataGoKrDividendInfo> records, int targetYear) {
        // 기준연도(baseYear) = 지급일 최소 연도. baseYear → targetYear로 매핑하고
        // 익년 지급분(예: 12월 결산 → 익년 4월)은 targetYear+1로 상대 연도 보존.
        // (buildPaymentEntries와 동일 로직: 단순 withYear(targetYear)는 두 해에 걸친
        //  지급일을 모두 올해로 눌러버림)
        // 기준연도 = 배당기준일(dvdnBasDt)의 최소 연도(없으면 지급일 연도 폴백).
        // 이 연도가 targetYear로 매핑되어 단일 결산 종목도 기준일이 올해로 변환됨.
        int baseYear = records.stream()
                .filter(r -> r.getBaseDt() != null)
                .mapToInt(r -> r.getBaseDt().getYear())
                .min()
                .orElseGet(() -> records.stream()
                        .filter(r -> r.getPayDate() != null)
                        .mapToInt(r -> r.getPayDate().getYear())
                        .min().orElse(targetYear));
        // baseYear → targetYear 균일 시프트. payDate·baseDt 모두 동일 적용해
        // 지급일-기준일 상대 관계 및 cross-year 보존.
        int shift = targetYear - baseYear;
        return records.stream()
                .filter(r -> r.getPayDate() != null)
                .map(r -> new DataGoKrDividendInfo(
                        r.getPayDate().withYear(r.getPayDate().getYear() + shift),
                        r.getAmountPerShare(),
                        r.getBaseDt() != null
                                ? r.getBaseDt().withYear(r.getBaseDt().getYear() + shift)
                                : null))
                .sorted(Comparator.comparing(r -> r.getPayDate().getMonthValue()))
                .collect(Collectors.toList());
    }
}
