package com.example.dividend.service;

import com.example.dividend.client.DataGoKrDividendClient;
import com.example.dividend.client.DataGoKrDividendInfo;
import com.example.dividend.dto.request.DividendAutoGenerateRequest;
import com.example.dividend.dto.request.DividendConfirmRequest;
import com.example.dividend.dto.request.DividendUpdateRequest;
import com.example.dividend.dto.request.UpdateScheduleRequest;
import com.example.dividend.dto.response.DividendAutoGenerateResponse;
import com.example.dividend.dto.response.StockDividendResponse;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import com.example.dividend.repository.TransactionRepository;
import com.example.dividend.util.KoreanBusinessDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DividendService {

    private final DividendRepository     dividendRepository;
    private final StockRepository        stockRepository;
    private final TransactionRepository  transactionRepository;
    private final DataGoKrDividendClient dataGoKrClient;

    // ── [1] 예상 배당 생성 (DB 기반 — API 호출 없음) ──────────────────────────

    /**
     * 작년 Dividend row 패턴을 올해로 복사.
     * API 호출 없음 — 이미 DB에 저장된 데이터만 사용.
     */
    @Transactional
    public List<Dividend> generateExpectedDividends(Long userId, Long stockId, Integer targetYear) {
        Stock stock = stockRepository.findByIdAndUser_Id(stockId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다"));

        int year = (targetYear != null) ? targetYear : LocalDate.now().getYear();

        // ① 보유수량 (Transaction 집계 — Stock.quantity 폴백 제거)
        int quantity = transactionRepository.calculateNetQuantity(stockId);
        if (quantity < 1) {
            log.warn("보유수량 0 — 배당 생성 건너뜀 [stockCode={}]", stock.getStockCode());
            return List.of();
        }

        // ② 작년 Dividend row → 올해 패턴 합성 (API 호출 없음)
        List<Dividend> lastYearRows = dividendRepository.findByUserIdAndStockIdAndYear(userId, stockId, year - 1);

        List<DataGoKrDividendInfo> syntheticRecords;
        String cycle;

        if (!lastYearRows.isEmpty()) {
            // 작년 실데이터 있음: 연도만 올해로 교체.
            // updateSchedule로 생성된 row는 paymentDate=null 이므로
            // 필터 없이 전체 처리하고 날짜가 없으면 해당 월 20일로 합성.
            syntheticRecords = lastYearRows.stream()
                    .map(r -> {
                        LocalDate base = r.getPaymentDate() != null
                                ? r.getPaymentDate().withYear(year)
                                : LocalDate.of(year, r.getMonth(), 20); // 날짜 합성
                        // 단가: CONFIRMED면 confirmedAmount 우선, 없으면 expectedAmount
                        BigDecimal amtBd = "CONFIRMED".equals(r.getStatus()) && r.getConfirmedAmount() != null
                                ? r.getConfirmedAmount() : r.getExpectedAmount();
                        int perShare = (amtBd != null && quantity > 0)
                                ? amtBd.intValue() / quantity : 0;
                        return new DataGoKrDividendInfo(base, perShare);
                    })
                    .sorted(Comparator.comparing(r -> r.getPayDate().getMonthValue()))
                    .collect(Collectors.toList());
            cycle = detectCycle(syntheticRecords.size());
        } else {
            // 작년 데이터 없음: Stock 엔티티 값으로 최후 폴백
            cycle = stock.getDividendCycle();
            if (cycle == null) cycle = "ANNUAL"; // 기본값
            syntheticRecords = buildFallbackRecords(cycle, year,
                    Optional.ofNullable(stock.getExpectedDividendPerShare()).orElse(0),
                    switch (cycle.toUpperCase()) {
                        case "QUARTERLY" -> 4;
                        case "MONTHLY"   -> 12;
                        default          -> 1;
                    });
            log.info("Stock 기본값으로 배당 폴백 생성 [stockCode={}, cycle={}, perShareAnnual={}]",
                    stock.getStockCode(), cycle, stock.getExpectedDividendPerShare());
        }

        // ③ 지급 월 목록 결정 (데이터 없으면 기존 유지)
        List<PaymentEntry> entries = buildPaymentEntries(cycle, syntheticRecords, year, stock.getStockCode());
        if (entries.isEmpty()) return List.of();

        // ④ 기존 EXPECTED row 삭제 후 재생성
        dividendRepository.deleteExpectedByUserIdAndStockIdAndYear(userId, stockId, year);

        // ⑦ Dividend row 생성
        return createDividendRows(userId, stockId, year, entries, stock, quantity);
    }

    /**
     * 배당 횟수 → 주기 판별.
     * @return "MONTHLY" | "QUARTERLY" | "ANNUAL" | null(판별 불가)
     */
    public String detectCycle(int count) {
        if (count >= 12) return "MONTHLY";
        if (count == 4)  return "QUARTERLY";
        if (count == 1)  return "ANNUAL";
        return null;
    }

    /**
     * dvdnBasDt(배당기준일) 월 간격으로 배당주기 판별.
     *
     * count 방식은 데이터가 불완전할 때 오판하지만,
     * 월 간격 방식은 레코드 수와 관계없이 패턴으로 판별합니다.
     *
     * 예) SK텔레콤: baseDt 월 [2, 5, 8] → 간격 [3, 3] → QUARTERLY
     *     삼성전자: baseDt 월 [3, 6, 9, 12] → 간격 [3, 3, 3] → QUARTERLY
     *
     * @return "MONTHLY" | "QUARTERLY" | "SEMI_ANNUAL" | "ANNUAL" | null
     */
    public String detectCycleFromBaseDtMonths(List<com.example.dividend.client.DataGoKrDividendInfo> records) {
        if (records == null || records.isEmpty()) return null;

        // baseDt 있는 레코드만 월 추출
        List<Integer> months = records.stream()
                .filter(r -> r.getBaseDt() != null)
                .map(com.example.dividend.client.DataGoKrDividendInfo::getBaseDtMonth)
                .filter(m -> m > 0)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        if (months.isEmpty()) return detectCycle(records.size()); // baseDt 없으면 count 방식 폴백
        if (months.size() == 1) return "ANNUAL";
        if (months.size() >= 12) return "MONTHLY";

        // 연속된 월 간격 계산
        java.util.Set<Integer> intervals = new java.util.LinkedHashSet<>();
        for (int i = 1; i < months.size(); i++) {
            intervals.add(months.get(i) - months.get(i - 1));
        }

        // 간격이 모두 동일한 경우
        if (intervals.size() == 1) {
            int gap = intervals.iterator().next();
            if (gap == 1) return "MONTHLY";
            if (gap == 3) return "QUARTERLY";
            if (gap == 6) return "SEMI_ANNUAL";
        }

        // 간격이 섞여도 평균으로 판별 (예: 결산월이 섞이는 경우)
        double avgGap = intervals.stream().mapToInt(i -> i).average().orElse(0);
        if (avgGap <= 1.5) return "MONTHLY";
        if (avgGap <= 4)   return "QUARTERLY";
        if (avgGap <= 7)   return "SEMI_ANNUAL";

        return "ANNUAL";
    }

    // ── [2] API 데이터로 배당 생성 (종목 등록 / 관리자 갱신 / 배치 전용) ─────

    /**
     * 사전에 조회한 API 레코드로 Dividend row 생성.
     * data.go.kr 호출은 호출자(StockService, DividendBatchService)가 담당.
     */
    @Transactional
    public List<Dividend> generateFromApiData(Long userId, Long stockId, int year,
            List<DataGoKrDividendInfo> validRecords, String cycle) {

        Stock stock = stockRepository.findByIdAndUser_Id(stockId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다"));

        int quantity = transactionRepository.calculateNetQuantity(stockId);
        if (quantity < 1) {
            log.warn("보유수량 0 — 배당 생성 건너뜀 [stockCode={}]", stock.getStockCode());
            return List.of();
        }

        List<PaymentEntry> entries = buildPaymentEntries(cycle, validRecords, year, stock.getStockCode());
        if (entries.isEmpty()) return List.of();

        dividendRepository.deleteExpectedByUserIdAndStockIdAndYear(userId, stockId, year);

        return createDividendRows(userId, stockId, year, entries, stock, quantity);
    }

    // ── [2-P] 과거 연도 배당 생성/재계산 (누적·연도별 그래프 전용) ─────────────

    /**
     * 과거 연도(거래 시작 ~ 작년)의 실제 수령 배당을 계산해 CONFIRMED row로 생성.
     *
     * - data.go.kr 전체 이력 1회 조회 (추가 트래픽 없음).
     * - 각 회차의 배당락일(기준일-1영업일) 시점 보유수량으로 정확히 계산.
     *   · 배당락일 당일 거래는 미포함 (strict <)
     *   · 보유 0 회차는 row 미생성
     * - 지급연도 기준으로 year/paymentDate 일관 설정 → 올해(현재연도) 집계엔 미반영.
     * - 올해(EXPECTED) 생성 로직과 완전 분리.
     */
    @Transactional
    public List<Dividend> generatePastDividends(Long userId, Long stockId) {
        Stock stock = stockRepository.findByIdAndUser_Id(stockId, userId).orElse(null);
        if (stock == null) return List.of();

        List<Transaction> txs = transactionRepository.findByUserIdAndStockId(userId, stockId);
        if (txs.isEmpty()) return List.of();

        int currentYear = LocalDate.now().getYear();

        List<DataGoKrDividendInfo> records =
                dataGoKrClient.fetchAllYearsDividendRecords(stock.getStockCode(), stock.getStockName());
        if (records.isEmpty()) return List.of();

        List<Dividend> created = new ArrayList<>();
        Set<String> seen = new HashSet<>(); // (year,month) 유니크 제약 방어

        for (DataGoKrDividendInfo rec : records) {
            LocalDate payDate = rec.getPayDate();
            if (payDate == null) continue;

            int payYear = payDate.getYear();
            if (payYear >= currentYear) continue;       // 올해 이상은 과거 아님 (올해 EXPECTED가 담당)
            if (rec.getAmountPerShare() <= 0) continue;

            // 배당락일 = 기준일-1영업일 (기준일 없으면 지급일-1영업일 근사)
            LocalDate exDate = rec.getBaseDt() != null
                    ? KoreanBusinessDay.prevBusinessDay(rec.getBaseDt())
                    : KoreanBusinessDay.prevBusinessDay(payDate);

            int qty = holdingBefore(txs, exDate);       // 배당락일 시점 보유수량 (당일 제외)
            if (qty <= 0) continue;                     // 미보유 회차 제외

            int month = payDate.getMonthValue();
            if (!seen.add(payYear + "-" + month)) continue;

            long amount = (long) rec.getAmountPerShare() * qty;
            Dividend d = new Dividend();
            d.setUserId(userId);
            d.setStockId(stockId);
            d.setYear(payYear);
            d.setMonth(month);
            d.setStatus("CONFIRMED");
            d.setConfirmedAmount(BigDecimal.valueOf(amount));
            d.setExpectedAmount(BigDecimal.valueOf(amount));
            d.setPaymentDate(payDate);
            d.setBaseDate(rec.getBaseDt());
            d.setExDividendDate(exDate);
            created.add(dividendRepository.save(d));
        }

        if (!created.isEmpty())
            log.info("과거 배당 생성 [stockCode={}, {}건]", stock.getStockCode(), created.size());
        return created;
    }

    /**
     * 거래 변경 시 과거 배당 재계산. 과거(작년 이하) row 전체 삭제 후 재생성.
     * 올해 row(EXPECTED 등)는 건드리지 않음.
     */
    @Transactional
    public void recalcPastDividends(Long userId, Long stockId) {
        int currentYear = LocalDate.now().getYear();
        dividendRepository.deletePastByUserIdAndStockId(userId, stockId, currentYear);
        generatePastDividends(userId, stockId);
    }

    /**
     * 배당락일 시점 보유수량 = 배당락일 이전(strict <) 거래의 BUY−SELL 합.
     * 배당락일 당일 거래는 미포함. (txs는 해당 종목 전 거래)
     */
    private int holdingBefore(List<Transaction> txs, LocalDate exDate) {
        if (exDate == null) return 0;
        return txs.stream()
                .filter(t -> t.getDate() != null && t.getDate().isBefore(exDate))
                .mapToInt(t -> "BUY".equals(t.getType()) ? t.getQuantity() : -t.getQuantity())
                .sum();
    }

    // ── [3] 배당 확정 처리 ────────────────────────────────────────────────────

    @Transactional
    public Dividend confirm(Long dividendId, Long userId, DividendConfirmRequest req) {
        Dividend dividend = dividendRepository.findByIdAndUserId(dividendId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "배당 정보를 찾을 수 없습니다: " + dividendId));
        // confirmedAmount > 0 → CONFIRMED, null/0 → EXPECTED 복귀
        if (req.getConfirmedAmount() != null && req.getConfirmedAmount() > 0) {
            dividend.setStatus("CONFIRMED");
            dividend.setConfirmedAmount(BigDecimal.valueOf(req.getConfirmedAmount()));
        } else {
            dividend.setStatus("EXPECTED");
            dividend.setConfirmedAmount(null);
        }
        // 사용자가 입력한 배당락일·지급일로 덮어쓰기 (null이면 기존값 유지)
        if (req.getExDividendDate() != null) dividend.setExDividendDate(req.getExDividendDate());
        if (req.getPaymentDate()    != null) dividend.setPaymentDate(req.getPaymentDate());
        // month는 확정 전환 시 절대 변경하지 않음.
        // 지급월이 바뀌면 monthly 집계가 어긋나므로 confirm API에서는 차단.

        return dividendRepository.save(dividend);
    }

    // ── [3] 월별 배당 요약 GET /dividend/monthly-summary ─────────────────────

    public Map<String, Object> getMonthlySummary(Long userId, int year) {
        Set<Long> activeIds = activeStockIds(userId);
        List<Dividend> dividends = dividendRepository.findByUserIdAndYear(userId, year).stream()
                .filter(d -> activeIds.contains(d.getStockId()))
                .collect(Collectors.toList());

        long[] estimated  = new long[13]; // index 1~12
        long[] confirmed  = new long[13];

        for (Dividend d : dividends) {
            int m = d.getMonth();
            if (m < 1 || m > 12) continue;
            if ("CONFIRMED".equals(d.getStatus())) {
                if (d.getConfirmedAmount() != null)
                    confirmed[m] += d.getConfirmedAmount().longValue();
            } else if ("EXPECTED".equals(d.getStatus())) {
                if (d.getExpectedAmount() != null
                        && d.getExpectedAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                    estimated[m] += d.getExpectedAmount().longValue();
            }
        }

        List<Map<String, Object>> months = new ArrayList<>();
        long totalEstimated = 0, totalConfirmed = 0;
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month",     m);
            row.put("estimated", estimated[m]);
            row.put("confirmed", confirmed[m]);
            months.add(row);
            totalEstimated += estimated[m];
            totalConfirmed += confirmed[m];
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year",           year);
        result.put("months",         months);
        result.put("totalEstimated", totalEstimated);
        result.put("totalConfirmed", totalConfirmed);
        return result;
    }

    // ── [4] 월별 배당금 조회 (기존 호환) ─────────────────────────────────────

    public List<Map<String, Object>> getMonthly(Long userId, int year) {
        Map<Long, Stock> stockMap = heldStockMap(userId); // 순보유 > 0 종목만 (거래 0 종목 제외)
        Map<Long, LocalDate> firstBuyMap = buildFirstBuyDateMap(userId);

        List<Dividend> dividends = dividendRepository.findByUserIdAndYear(userId, year).stream()
                .filter(d -> stockMap.containsKey(d.getStockId()))
                .collect(Collectors.toList());

        long[] expected = new long[13], confirmed = new long[13];
        for (Dividend d : dividends) {
            int m = d.getMonth();
            if (m < 1 || m > 12) continue;
            // 컷오프 2: payment_date 연도가 현재 연도인 것만 (2027 등 익년 지급분 제외)
            if (d.getPaymentDate() != null && d.getPaymentDate().getYear() != year) continue;

            if ("CONFIRMED".equals(d.getStatus())) {
                if (d.getConfirmedAmount() != null) confirmed[m] += d.getConfirmedAmount().longValue();
                if (d.getExpectedAmount()  != null) expected[m]  += d.getExpectedAmount().longValue();
            } else if ("EXPECTED".equals(d.getStatus()) && d.getExpectedAmount() != null) {
                // 컷오프 1: 첫 매수일 이후 배당락일인 것만 예상 합산
                if (isReceivable(d, firstBuyMap.get(d.getStockId())))
                    expected[m] += d.getExpectedAmount().longValue();
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", m); row.put("expectedAmount", expected[m]); row.put("confirmedAmount", confirmed[m]);
            result.add(row);
        }
        return result;
    }

    // ── [5] 연간 예상 배당금 ──────────────────────────────────────────────────

    public Map<String, Object> getAnnual(Long userId, int year) {
        Map<Long, Stock> stockMap = heldStockMap(userId); // 순보유 > 0 종목만 (거래 0 종목 제외)
        Map<Long, LocalDate> firstBuyMap = buildFirstBuyDateMap(userId);

        long total = dividendRepository.findByUserIdAndYear(userId, year).stream()
                .filter(d -> stockMap.containsKey(d.getStockId()))
                .mapToLong(d -> {
                    // 컷오프 2: payment_date 연도가 현재 연도인 것만
                    if (d.getPaymentDate() != null && d.getPaymentDate().getYear() != year) return 0L;
                    if ("CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null)
                        return d.getConfirmedAmount().longValue();
                    if ("EXPECTED".equals(d.getStatus()) && d.getExpectedAmount() != null) {
                        // 컷오프 1: 첫 매수일 이후 배당락일인 것만
                        return isReceivable(d, firstBuyMap.get(d.getStockId()))
                                ? d.getExpectedAmount().longValue() : 0L;
                    }
                    return 0L;
                }).sum();
        return Map.of("year", year, "totalExpectedAmount", total);
    }

    // ── [6] 누적 배당금 조회 ──────────────────────────────────────────────────

    public Map<String, Object> getCumulative(Long userId) {
        // JPQL 집계 쿼리의 @SQLRestriction 적용 불안정 회피 → Java 레벨 집계
        Set<Long> heldIds = heldStockIds(userId); // 순보유 > 0 종목만 (거래 0 종목 제외)
        List<Dividend> all = dividendRepository.findByUserId(userId).stream()
                .filter(d -> heldIds.contains(d.getStockId()))
                .collect(Collectors.toList());
        long totalConfirmed = all.stream()
                .filter(d -> "CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null)
                .mapToLong(d -> d.getConfirmedAmount().longValue()).sum();
        long totalExpected = all.stream()
                .mapToLong(d -> {
                    if ("CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null)
                        return d.getConfirmedAmount().longValue();
                    if ("EXPECTED".equals(d.getStatus()) && d.getExpectedAmount() != null)
                        return d.getExpectedAmount().longValue();
                    return 0L;
                }).sum();
        return Map.of("totalConfirmedAmount", totalConfirmed, "totalExpectedAmount", totalExpected);
    }

    // ── [7] 연도별 배당금 조회 ────────────────────────────────────────────────

    public List<Map<String, Object>> getYearly(Long userId) {
        // JPQL 집계 쿼리의 @SQLRestriction 적용 불안정 회피 → Java 레벨 집계
        Set<Long> heldIds = heldStockIds(userId); // 순보유 > 0 종목만 (거래 0 종목 제외)
        Map<Long, LocalDate> firstBuyMap = buildFirstBuyDateMap(userId);
        List<Dividend> all = dividendRepository.findByUserId(userId).stream()
                .filter(d -> heldIds.contains(d.getStockId()))
                .collect(Collectors.toList());

        // 연도별 그룹핑
        Map<Integer, long[]> byYear = new TreeMap<>(); // [0]=confirmed, [1]=expected
        for (Dividend d : all) {
            int year = d.getYear();
            if (d.getPaymentDate() != null && d.getPaymentDate().getYear() != year) continue;
            long[] sums = byYear.computeIfAbsent(year, k -> new long[]{0L, 0L});
            if ("CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null) {
                sums[0] += d.getConfirmedAmount().longValue();
                sums[1] += d.getConfirmedAmount().longValue();
            } else if ("EXPECTED".equals(d.getStatus()) && d.getExpectedAmount() != null) {
                if (isReceivable(d, firstBuyMap.get(d.getStockId())))
                    sums[1] += d.getExpectedAmount().longValue();
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, long[]> entry : byYear.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("year",            entry.getKey());
            item.put("confirmedAmount", entry.getValue()[0]);
            item.put("expectedAmount",  entry.getValue()[1]);
            result.add(item);
        }
        return result;
    }

    // ── [8] 확정 전환 드롭다운용 종목 목록 ──────────────────────────────────────

    public List<Map<String, Object>> getStocksForConfirm(Long userId, int year) {
        LocalDate today = LocalDate.now();
        Set<Long> activeIds = activeStockIds(userId);
        List<Dividend> upcoming = dividendRepository.findUpcomingWithPaymentDate(userId, year, today)
                .stream()
                .filter(d -> activeIds.contains(d.getStockId()))
                .collect(Collectors.toList());

        Map<Long, List<Dividend>> byStock = new LinkedHashMap<>();
        for (Dividend d : upcoming) {
            byStock.computeIfAbsent(d.getStockId(), k -> new ArrayList<>()).add(d);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Dividend>> entry : byStock.entrySet()) {
            Long stockId = entry.getKey();
            List<Dividend> divs = entry.getValue();

            Stock stock = stockRepository.findById(stockId).orElse(null);
            if (stock == null) continue;

            List<Map<String, Object>> upcomingMonths = new ArrayList<>();
            for (Dividend d : divs) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("dividendId",  d.getId());
                m.put("month",       d.getMonth());
                m.put("paymentDate", d.getPaymentDate().toString());
                m.put("status",      d.getStatus());
                upcomingMonths.add(m);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stockId",        stockId);
            item.put("stockName",      stock.getStockName());
            item.put("stockCode",      stock.getStockCode());
            item.put("dividendCycle",  stock.getDividendCycle());
            item.put("upcomingMonths", upcomingMonths);
            result.add(item);
        }
        return result;
    }

    // ── [9] 배당 스케줄 일괄 업데이트 ────────────────────────────────────────

    @Transactional
    public List<Dividend> updateSchedule(Long userId, UpdateScheduleRequest req) {
        int year = LocalDate.now().getYear();

        Stock stock = stockRepository.findByIdAndUser_Id(req.getStockId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다"));

        int qty = transactionRepository.calculateNetQuantity(req.getStockId());
        // 보유 이력이 전혀 없는 종목(거래 0건)은 차단. 전량 매도(거래 있고 net 0)는 허용.
        if (qty <= 0 && transactionRepository.findByUserIdAndStockId(userId, req.getStockId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보유 이력이 없는 종목은 배당을 확정할 수 없습니다");
        }
        if (qty < 1) qty = 1;

        BigDecimal confirmedAmt = BigDecimal.valueOf((long) req.getDividendPerShare() * qty);

        // 지급월 배열 계산
        List<Integer> targetMonths = calcScheduleMonths(req.getFirstPaymentMonth(), req.getDividendCycle());

        // 기존 row 전체 삭제 후 재생성
        dividendRepository.deleteByUserIdAndStockIdAndYear(userId, req.getStockId(), year);

        // ── stock.dividendCycle 동기화 ──
        // updateSchedule 호출 시 사용자가 명시적으로 cycle을 지정했으므로 엔티티도 갱신
        if (req.getDividendCycle() != null) {
            stock.setDividendCycle(req.getDividendCycle());
            stockRepository.save(stock);
        }

        int first = req.getFirstPaymentMonth();
        List<Dividend> result = new ArrayList<>();
        for (Integer month : targetMonths) {
            Dividend d = new Dividend();
            d.setUserId(userId);
            d.setStockId(req.getStockId());
            d.setYear(year);
            d.setMonth(month);
            d.setStatus("CONFIRMED");
            d.setConfirmedAmount(confirmedAmt);
            d.setExpectedAmount(confirmedAmt);
            result.add(dividendRepository.save(d));
        }

        log.info("배당 스케줄 업데이트 [stockCode={}, firstMonth={}, cycle={}, months={}]",
                stock.getStockCode(), first, req.getDividendCycle(), targetMonths);
        return result;
    }

    /** 첫 지급월 + 배당주기 → 지급월 배열 */
    private List<Integer> calcScheduleMonths(int first, String cycle) {
        if (cycle == null) return List.of(first);
        return switch (cycle.toUpperCase()) {
            case "QUARTERLY"   -> buildMonthSeq(first, 3, 4);
            case "SEMI_ANNUAL" -> buildMonthSeq(first, 6, 2);
            case "MONTHLY"     -> java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList();
            default            -> List.of(first); // ANNUAL
        };
    }

    private List<Integer> buildMonthSeq(int first, int interval, int count) {
        List<Integer> months = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            months.add(((first - 1 + i * interval) % 12) + 1);
        }
        return months;
    }

    // ── [10] 전체 배당 조회 ────────────────────────────────────────────────────

    public List<Dividend> getAll(Long userId) {
        // JPQL EXISTS 서브쿼리의 @SQLRestriction 적용 불안정 문제 회피:
        // stockRepository.findByUser_Id()는 @SQLRestriction("deleted_at IS NULL")이
        // 확실하게 적용되므로, 여기서 얻은 활성 stockId Set으로 Java 레벨에서 필터링.
        Set<Long> activeIds = activeStockIds(userId);
        return dividendRepository.findByUserId(userId).stream()
                .filter(d -> activeIds.contains(d.getStockId()))
                .collect(Collectors.toList());
    }

    /** 소프트딜리트되지 않은 사용자 종목 ID 집합 반환 (서비스 내 공통 필터용) */
    private Set<Long> activeStockIds(Long userId) {
        return stockRepository.findByUser_Id(userId).stream()
                .map(Stock::getId)
                .collect(Collectors.toSet());
    }

    /** 소프트딜리트되지 않은 사용자 종목 Map<stockId, Stock> (createdAt 접근 + N+1 회피) */
    private Map<Long, Stock> activeStockMap(Long userId) {
        return stockRepository.findByUser_Id(userId).stream()
                .collect(Collectors.toMap(Stock::getId, s -> s));
    }

    /**
     * 거래기반 순보유수량 Map<stockId, qty> — 소프트딜리트 제외 + 순보유 > 0 인 종목만.
     * 배당 조회 5종(getAnnual/getCumulative/getMonthly/getYearly/getByStock)의 보유 게이트 공통 소스.
     * Transaction을 userId 스코프로 1회만 순회해 합산 (N+1 및 비-userId 스코프 calculateNetQuantity 회피).
     */
    private Map<Long, Integer> heldQtyMap(Long userId) {
        Set<Long> activeIds = activeStockIds(userId);
        Map<Long, Integer> netQty = new HashMap<>();
        for (Transaction t : transactionRepository.findByUserId(userId)) {
            if (!activeIds.contains(t.getStockId())) continue;
            int signed = "BUY".equals(t.getType()) ? t.getQuantity() : -t.getQuantity();
            netQty.merge(t.getStockId(), signed, Integer::sum);
        }
        netQty.values().removeIf(q -> q <= 0); // 순보유 0 이하(미보유·전량 매도) 제외
        return netQty;
    }

    /** 거래기반 순보유수량 > 0 인 종목 ID 집합 (보유 게이트). */
    private Set<Long> heldStockIds(Long userId) {
        return heldQtyMap(userId).keySet();
    }

    /** 거래기반 순보유수량 > 0 인 종목 Map<stockId, Stock> (보유 게이트 + createdAt 접근). */
    private Map<Long, Stock> heldStockMap(Long userId) {
        Set<Long> held = heldStockIds(userId);
        return activeStockMap(userId).entrySet().stream()
                .filter(e -> held.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 배당락일 계산 (우선순위: 저장된 exDividendDate → baseDate-1영업일 → paymentDate-1영업일 근사).
     * getByStock 응답과 컷오프 판별에 공통 사용.
     */
    private LocalDate computeExDate(Dividend d) {
        if (d.getExDividendDate() != null) return d.getExDividendDate();
        if (d.getBaseDate() != null) return KoreanBusinessDay.prevBusinessDay(d.getBaseDate());
        if (d.getPaymentDate() != null) return KoreanBusinessDay.prevBusinessDay(d.getPaymentDate());
        return null;
    }

    // ── 배당락일 추정 유틸 (배당기준일 미제공 시 지급일 역산) ────────────────────

    /** 지급일 앵커와 같은 연도 간격으로 날짜를 대상 연도에 맞춤 */
    private static LocalDate shiftToTargetYear(LocalDate date, int targetYear) {
        return date.plusYears((long) targetYear - date.getYear());
    }

    private static LocalDate shiftToTargetYear(LocalDate date, int targetYear, LocalDate anchorPayDate) {
        return date.plusYears((long) targetYear - anchorPayDate.getYear());
    }

    /**
     * 공공데이터 배당기준일(dvdnBasDt) → 예상 배당락일.
     * 기준일 없으면 지급일 기준 약 2개월 전으로 추정.
     */
    private static LocalDate resolveExDividendDate(LocalDate baseDt, LocalDate anchorPayDate, int targetYear) {
        if (baseDt != null && anchorPayDate != null) {
            return shiftToTargetYear(baseDt, targetYear, anchorPayDate);
        }
        if (anchorPayDate != null) {
            return estimateExDateFromPayDate(shiftToTargetYear(anchorPayDate, targetYear));
        }
        return null;
    }

    /** 배당기준일 미제공 시 지급일 역산 (한국 시장 관행: 약 2개월 전) */
    private static LocalDate estimateExDateFromPayDate(LocalDate payDate) {
        if (payDate == null) return null;
        return payDate.minusMonths(2);
    }

    /**
     * EXPECTED 배당 수령 자격 판별 (개선판).
     * 비교 날짜는 배당락일(exDividendDate) 우선, 없으면 지급일(paymentDate).
     * - 첫 매수일 null → 제외 (거래내역 없음 = 미보유)
     * - 비교 날짜 null → 포함 (보수적)
     * - 비교 날짜 >= 첫 매수일 → 포함
     */
    private boolean isExpectedReceivable(Dividend d, LocalDate firstBuyDate) {
        LocalDate cutoffDate = d.getExDividendDate() != null
                ? d.getExDividendDate() : d.getPaymentDate();
        if (cutoffDate == null) return true;
        return firstBuyDate != null && !cutoffDate.isBefore(firstBuyDate);
    }

    /**
     * 수령 가능 여부: 배당락일이 null이거나 첫 매수일 이후이면 true.
     * - EXPECTED 배당의 예상 합산 포함 여부를 동적 판별하는 컷오프 1.
     */
    private boolean isReceivable(Dividend d, LocalDate firstBuyDate) {
        LocalDate exDate = computeExDate(d);
        // 배당락일 null → 보수적으로 포함. 첫 매수일 null(거래 없음) → 포함(보유수량 0이라 실제 표시 안 됨)
        return exDate == null || firstBuyDate == null || !exDate.isBefore(firstBuyDate);
    }

    /** 사용자 전체 종목의 첫 BUY 거래일 Map (N+1 방지). stockId → 최초 매수일. */
    private Map<Long, LocalDate> buildFirstBuyDateMap(Long userId) {
        Map<Long, LocalDate> result = new HashMap<>();
        for (Transaction t : transactionRepository.findByUserId(userId)) {
            if ("BUY".equals(t.getType()) && t.getDate() != null) {
                result.merge(t.getStockId(), t.getDate(),
                        (a, b) -> a.isBefore(b) ? a : b);
            }
        }
        return result;
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    /** 지급 월 + 지급일 + 주당배당금 + 배당기준일 묶음 */
    private record PaymentEntry(int month, LocalDate payDate, int amountPerShare, LocalDate baseDate) {}

    /**
     * API·DB 데이터 모두 없을 때 Stock 기본값으로 합성 레코드 생성.
     * 한국 시장 관행: 지급월 = 기준월 + 2개월 (12월 결산 → 다음해 4월)
     */
    private List<DataGoKrDividendInfo> buildFallbackRecords(
            String cycle, int year, int annualPerShare, int numPayments) {

        List<Integer> recordMonths = switch (cycle.toUpperCase()) {
            case "QUARTERLY" -> List.of(3, 6, 9, 12);
            case "MONTHLY"   -> List.of(1,2,3,4,5,6,7,8,9,10,11,12);
            default          -> List.of(12);
        };

        int perShare = numPayments > 0 ? annualPerShare / numPayments : 0;
        List<DataGoKrDividendInfo> records = new ArrayList<>();

        for (int recMonth : recordMonths) {
            int payMonthOffset = (recMonth == 12) ? 4 : 2;
            int payMonth = recMonth + payMonthOffset;
            int payYear  = year;
            if (payMonth > 12) { payMonth -= 12; payYear++; }
            LocalDate payDate = LocalDate.of(payYear, payMonth, 20);
            records.add(new DataGoKrDividendInfo(payDate, perShare));
        }
        return records;
    }

    /** PaymentEntry 목록으로 Dividend row 저장 (공통) */
    private List<Dividend> createDividendRows(Long userId, Long stockId, int year,
            List<PaymentEntry> entries, Stock stock, int quantity) {
        List<Dividend> created = new ArrayList<>();
        for (PaymentEntry entry : entries) {
            if (dividendRepository.existsByUserIdAndStockIdAndYearAndMonth(
                    userId, stockId, year, entry.month)) continue;

            int perShare;
            if (entry.amountPerShare > 0) {
                perShare = entry.amountPerShare;
            } else {
                int annualPerShare = Optional.ofNullable(stock.getExpectedDividendPerShare()).orElse(0);
                perShare = annualPerShare / Math.max(entries.size(), 1);
            }
            BigDecimal expectedAmount = perShare > 0
                    ? BigDecimal.valueOf((long) perShare * quantity)
                    : BigDecimal.ZERO;

            Dividend d = new Dividend();
            d.setUserId(userId);
            d.setStockId(stockId);
            d.setYear(year);
            d.setMonth(entry.month);
            d.setExpectedAmount(expectedAmount);
            d.setConfirmedAmount(null);
            d.setStatus("EXPECTED");
            d.setPaymentDate(entry.payDate);
            // 배당기준일 저장 + 배당락일(= 기준일 - 1영업일) 계산
            d.setBaseDate(entry.baseDate);
            d.setExDividendDate(entry.baseDate != null
                    ? KoreanBusinessDay.prevBusinessDay(entry.baseDate)
                    : null);

            created.add(dividendRepository.save(d));
            log.info("Dividend row 생성 [stockCode={}, year={}, month={}, 예상금액={}, 지급일={}, 기준일={}, 배당락일={}]",
                    stock.getStockCode(), year, entry.month, expectedAmount, entry.payDate,
                    entry.baseDate, d.getExDividendDate());
        }
        return created;
    }

    /**
     * API 레코드를 바탕으로 PaymentEntry 목록 생성.
     * API 데이터가 있을 때만 실제 지급일(월) 기반으로 생성.
     * API 데이터 없으면 빈 목록 반환 (고정 패턴 사용 안 함).
     */
    private List<PaymentEntry> buildPaymentEntries(
            String cycle, List<DataGoKrDividendInfo> validRecords, int year, String stockCode) {

        if (validRecords.isEmpty()) {
            log.info("API 데이터 없음 — 배당 row 미생성 [stockCode={}, cycle={}]", stockCode, cycle);
            return List.of();
        }

        // API 데이터 있음: 실제 지급일을 올해 기준으로 변환하되 상대 연도 보존.
        // 기준연도(baseYear) = 레코드 지급일들의 최소 연도. 이 연도가 targetYear(올해)로
        // 매핑되고, 익년으로 넘어간 지급일(예: 12월 결산 → 익년 4월)은 targetYear+1로 보존한다.
        // (단순 withYear(year)는 두 해에 걸친 지급일을 모두 올해로 눌러버리는 버그가 있었음)
        // 기준연도 = 배당기준일(dvdnBasDt)의 최소 연도. 이 연도가 targetYear(올해)로
        // 매핑되도록 shift를 잡으면, 12월 결산처럼 익년에 지급되는 종목도
        // 기준일은 올해로, 지급일은 익년으로 일관되게 변환된다.
        // (지급일 연도를 기준으로 잡으면 단일 결산 종목에서 shift=0이 되어
        //  기준일이 작년에 남는 버그가 있었음). baseDt 없으면 지급일 연도로 폴백.
        int baseYear = validRecords.stream()
                .filter(r -> r.getBaseDt() != null)
                .mapToInt(r -> r.getBaseDt().getYear())
                .min()
                .orElseGet(() -> validRecords.stream()
                        .filter(r -> r.getPayDate() != null)
                        .mapToInt(r -> r.getPayDate().getYear())
                        .min().orElse(year));

        // baseYear → targetYear(year) 균일 시프트. payDate·baseDt 모두 동일 기준 적용
        // (지급일과 배당기준일의 상대 관계 및 cross-year 보존).
        int shift = year - baseYear;

        List<PaymentEntry> entries = new ArrayList<>();
        for (DataGoKrDividendInfo rec : validRecords) {
            LocalDate thisYearPayDate = rec.getPayDate().withYear(rec.getPayDate().getYear() + shift);
            LocalDate thisYearBaseDt  = rec.getBaseDt() != null
                    ? rec.getBaseDt().withYear(rec.getBaseDt().getYear() + shift)
                    : null;
            entries.add(new PaymentEntry(
                    thisYearPayDate.getMonthValue(),
                    thisYearPayDate,
                    rec.getAmountPerShare(),
                    thisYearBaseDt
            ));
        }
        return entries;
    }

    // ── [NEW-1] 단일 분기 확정 (나머지 분기 자동생성 제거) ──────────────────────
    // 입력된 dividendId row 1건만 CONFIRMED 처리.
    // 나머지 분기는 기존 EXPECTED row를 그대로 유지 — 사용자가 수정 모달로 개별 확정.

    @Transactional
    public DividendAutoGenerateResponse confirmWithAutoGenerate(Long userId, DividendAutoGenerateRequest req) {
        Stock stock = stockRepository.findByIdAndUser_Id(req.getStockId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다"));

        // 보유 이력이 전혀 없는 종목(거래 0건)에 CONFIRMED 배당 생성 차단.
        // 단, 과거 보유 후 전량 매도(거래는 있으나 net 0)는 실수령 기록일 수 있어 허용.
        int net = transactionRepository.calculateNetQuantity(req.getStockId());
        if (net <= 0 && transactionRepository.findByUserIdAndStockId(userId, req.getStockId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보유 이력이 없는 종목은 배당을 확정할 수 없습니다");
        }
        int qty = Math.max(net, 1);
        LocalDate paymentDate = req.getFirstPaymentDate();
        long dividendAmount = req.getDividendAmount();

        int year = paymentDate.getYear();
        int month = paymentDate.getMonthValue();
        List<Dividend> existingRows = dividendRepository.findByUserIdAndStockIdAndYear(userId, req.getStockId(), year);

        BigDecimal totalAmount = BigDecimal.valueOf(dividendAmount * qty);

        // 선택한 분기 row 1건만 CONFIRMED
        Dividend row = existingRows.stream()
                .filter(d -> d.getMonth() == month).findFirst().orElse(null);
        if (row == null) {
            row = new Dividend();
            row.setUserId(userId); row.setStockId(req.getStockId());
            row.setYear(year); row.setMonth(month);
        }
        row.setStatus("CONFIRMED");
        row.setConfirmedAmount(totalAmount);
        row.setExpectedAmount(totalAmount);
        row.setPaymentDate(paymentDate);
        if (req.getExDividendDate() != null) row.setExDividendDate(req.getExDividendDate());
        dividendRepository.save(row);

        Map<String, Object> confirmed = new LinkedHashMap<>();
        confirmed.put("paymentDate", paymentDate.toString());
        confirmed.put("amount", dividendAmount);
        confirmed.put("status", "CONFIRMED");

        // 나머지 분기는 건드리지 않음 (빈 리스트 반환)
        return new DividendAutoGenerateResponse(confirmed, List.of());
    }

    // ── [NEW-2] 개별 배당 row 업데이트 ──────────────────────────────────────

    @Transactional
    public Dividend updateDividend(Long dividendId, Long userId, DividendUpdateRequest req) {
        Dividend dividend = dividendRepository.findByIdAndUserId(dividendId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "배당 정보를 찾을 수 없습니다: " + dividendId));
        if (req.getExDividendDate() != null) dividend.setExDividendDate(req.getExDividendDate());
        if (req.getPaymentDate()    != null) dividend.setPaymentDate(req.getPaymentDate());
        if ("CONFIRMED".equals(req.getStatus())) {
            dividend.setStatus("CONFIRMED");
            if (req.getAmount() != null) {
                dividend.setConfirmedAmount(BigDecimal.valueOf(req.getAmount()));
                dividend.setExpectedAmount(BigDecimal.valueOf(req.getAmount()));
            }
        } else {
            if (req.getAmount() != null) dividend.setExpectedAmount(BigDecimal.valueOf(req.getAmount()));
        }
        return dividendRepository.save(dividend);
    }

    // ── [NEW-3] 종목별 배당 정보 조회 (paymentDate 기준, null 제외) ───────────

    public List<StockDividendResponse> getByStock(Long userId, int year) {
        Map<Long, Integer> qtyMap = heldQtyMap(userId);   // 순보유 > 0 종목 + 수량 (공통 보유 게이트)
        Map<Long, Stock> stockMap = heldStockMap(userId); // 거래 0 종목 제외
        Map<Long, LocalDate> firstBuyMap = buildFirstBuyDateMap(userId);

        // 컷오프 2: paymentDate 연도가 현재 연도인 것만 (2027 등 익년 지급분 미표시)
        List<Dividend> dividends = dividendRepository.findByUserId(userId).stream()
                .filter(d -> stockMap.containsKey(d.getStockId()))
                .filter(d -> d.getPaymentDate() != null)
                .filter(d -> d.getPaymentDate().getYear() == year)
                .collect(Collectors.toList());

        Map<Long, List<Dividend>> byStock = new LinkedHashMap<>();
        for (Dividend d : dividends) byStock.computeIfAbsent(d.getStockId(), k -> new ArrayList<>()).add(d);

        List<StockDividendResponse> result = new ArrayList<>();
        for (Map.Entry<Long, List<Dividend>> entry : byStock.entrySet()) {
            Long stockId = entry.getKey();
            List<Dividend> divs = entry.getValue();
            Stock stock = stockMap.get(stockId);
            if (stock == null) continue;

            // 보유 게이트(heldStockMap)로 이미 순보유 > 0 보장 → qtyMap 값 사용
            int qty = qtyMap.getOrDefault(stockId, 1);
            LocalDate firstBuyDate = firstBuyMap.get(stockId);

            List<Map<String, Object>> paymentDates = divs.stream()
                    .sorted(Comparator.comparing(Dividend::getPaymentDate))
                    .map(d -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("dividendId",   d.getId());
                        m.put("paymentMonth", d.getPaymentDate().getMonthValue());
                        m.put("paymentDate",  d.getPaymentDate().toString());
                        m.put("paymentYear",  d.getPaymentDate().getYear());
                        LocalDate baseDate = d.getBaseDate();
                        LocalDate exDate = computeExDate(d);
                        m.put("baseDate",       baseDate != null ? baseDate.toString() : null);
                        m.put("exDividendDate", exDate != null ? exDate.toString() : null);
                        // 컷오프 1: 배당락일 >= 첫 매수일 → isReceivable
                        boolean receivable = isReceivable(d, firstBuyDate);
                        m.put("isReceivable", receivable);
                        // 주당 금액 + 총 금액 (프론트에서 개별 행 표시용)
                        long perShareAmt = "CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null
                                ? d.getConfirmedAmount().longValue() / qty
                                : d.getExpectedAmount() != null ? d.getExpectedAmount().longValue() / qty : 0L;
                        long totalAmt = "CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null
                                ? d.getConfirmedAmount().longValue()
                                : d.getExpectedAmount() != null ? d.getExpectedAmount().longValue() : 0L;
                        m.put("amount",      perShareAmt);
                        m.put("totalAmount", totalAmt);
                        m.put("status", d.getStatus());
                        return m;
                    })
                    .collect(Collectors.toList());

            long confirmedCount = divs.stream().filter(d -> "CONFIRMED".equals(d.getStatus())).count();
            long expectedCount  = divs.stream().filter(d -> "EXPECTED".equals(d.getStatus())).count();
            String stockStatus = confirmedCount > 0 && expectedCount == 0 ? "CONFIRMED"
                               : confirmedCount > 0 ? "PARTIAL_CONFIRMED" : "EXPECTED";

            // 예상 합산: CONFIRMED는 항상, EXPECTED는 첫 매수일 이후 배당락일인 것만
            long totalExpected = divs.stream().mapToLong(d -> {
                if ("CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null)
                    return d.getConfirmedAmount().longValue();
                if ("EXPECTED".equals(d.getStatus()) && d.getExpectedAmount() != null
                        && isReceivable(d, firstBuyDate))
                    return d.getExpectedAmount().longValue();
                return 0L;
            }).sum();

            int perShare = stock.getExpectedDividendPerShare() != null ? stock.getExpectedDividendPerShare() : 0;

            result.add(new StockDividendResponse(stockId, stock.getStockName(),
                    stock.getDividendCycle(), perShare, paymentDates, stockStatus, totalExpected));
        }
        return result;
    }
}
