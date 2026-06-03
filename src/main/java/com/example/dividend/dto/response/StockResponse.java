package com.example.dividend.dto.response;

import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.StockSector;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class StockResponse {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Long id;
    private Long userId;
    private String stockName;
    private String stockCode;
    private String sectorCode;
    private String sectorLabel;
    private String exchange;
    private String currency;
    private int quantity;
    private BigDecimal avgPrice;
    private BigDecimal previousClose;

    // ── 문서 요구 필드 ─────────────────────────────────
    /** 평가 금액 = 수량 × 전일 종가 (종가 없으면 null) */
    private BigDecimal evaluationAmount;
    /** 평가 손익 = 평가금액 - 투자원금 (종가 없으면 null) */
    private BigDecimal evaluationProfit;
    /** 등락률(%) = 손익 / 원금 × 100 (종가 없으면 null) */
    private BigDecimal profitRate;
    /** 투자 비중(%) = 본 종목 원금 / 전체 원금 × 100 */
    private BigDecimal investmentWeight;
    /** 배당기준월 목록 (지급일 오름차순, 월+연도 포함) */
    private List<PaymentMonthDto> paymentMonths;
    /** 실제 배당 지급일 목록 (paymentDate 기준, 오름차순) */
    private List<LocalDate> paymentDates;
    /** 올해 예상·확정 배당금 합산 */
    private Integer expectedDividend;

    // ── 신규 필드 ──────────────────────────────────────
    /** 배당 주기: ANNUAL / QUARTERLY / MONTHLY */
    private String dividendCycle;
    /** 주당 예상 배당금 */
    private Integer expectedDividendPerShare;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── 기본 변환 (단건 조회용 — 투자비중·배당 미포함) ──
    // netQty / txAvgPrice: Transaction 집계값. 거래내역 없으면 0 / ZERO 전달.
    public static StockResponse from(Stock stock, int netQty, BigDecimal txAvgPrice) {
        return of(stock, List.of(), BigDecimal.ZERO, netQty, txAvgPrice);
    }

    /** @deprecated 거래내역 미전달 — 보유수량 0으로 표시됨. 종목 등록 직후 응답에만 사용. */
    @Deprecated
    public static StockResponse from(Stock stock) {
        return of(stock, List.of(), BigDecimal.ZERO, 0, BigDecimal.ZERO);
    }

    // ── 전체 조회용 (투자비중·배당 포함) ──────────────
    // netQty, txAvgPrice: Transaction 집계 기반 보유수량·평균단가 (HoldingService 계산값)
    public static StockResponse of(Stock stock,
                                   List<Dividend> dividends,
                                   BigDecimal totalInvestment,
                                   int netQty,
                                   BigDecimal txAvgPrice) {
        StockSector sector = stock.getSector();
        // Transaction 집계 기반 수량·평균단가 사용 (Stock.quantity/avgPrice 무시)
        BigDecimal qty = BigDecimal.valueOf(Math.max(netQty, 0));
        BigDecimal avgPx = txAvgPrice != null ? txAvgPrice : BigDecimal.ZERO;
        BigDecimal investment = avgPx.multiply(qty);

        // 평가 계산
        BigDecimal evalAmount = null, evalProfit = null, profitRate = null;
        BigDecimal close = stock.getPreviousClose();
        if (close != null && close.compareTo(BigDecimal.ZERO) > 0) {
            evalAmount = close.multiply(qty).setScale(0, RoundingMode.DOWN);
            evalProfit = evalAmount.subtract(investment).setScale(0, RoundingMode.DOWN);
            if (investment.compareTo(BigDecimal.ZERO) > 0) {
                profitRate = evalProfit
                        .divide(investment, 6, RoundingMode.HALF_UP)
                        .multiply(HUNDRED)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        // 투자 비중
        BigDecimal weight = null;
        if (totalInvestment != null && totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            weight = investment
                    .divide(totalInvestment, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 배당금·배당주기·지급월: paymentDate 기준으로 올해+내년 포함
        int currentYear = LocalDate.now().getYear();

        // paymentDate가 있는 row만 사용 (올해 및 내년 지급 포함)
        List<Dividend> rowsWithPayDate = dividends.stream()
                .filter(d -> d.getPaymentDate() != null)
                .filter(d -> d.getPaymentDate().getYear() == currentYear
                          || d.getPaymentDate().getYear() == currentYear + 1)
                .toList();

        // paymentDate 없는 row는 year 기준 폴백
        List<Dividend> thisYear = dividends.stream()
                .filter(d -> d.getYear() == currentYear)
                .toList();

        boolean hasValidThisYear = !rowsWithPayDate.isEmpty()
                || thisYear.stream().anyMatch(d ->
                    "CONFIRMED".equals(d.getStatus())
                    || (d.getExpectedAmount() != null
                        && d.getExpectedAmount().compareTo(java.math.BigDecimal.ZERO) > 0));

        // baseRows: paymentDate 있는 row 우선, 없으면 year 기준 폴백
        List<Dividend> baseRows = !rowsWithPayDate.isEmpty()
                ? rowsWithPayDate
                : (hasValidThisYear ? thisYear
                    : dividends.stream().filter(d -> d.getYear() == currentYear - 1).toList());

        // 지급월 목록: paymentDate 있으면 지급일 기준(월+연도), 없으면 month/year 필드.
        // 연도가 다른 지급일(예: 다음해 1분기)을 프론트에서 구분 표시할 수 있도록
        // 월만 추출하지 않고 연도를 함께 담는다. 정렬은 지급일 오름차순(연→월).
        List<PaymentMonthDto> paymentMonths = baseRows.stream()
                .map(d -> d.getPaymentDate() != null
                        ? new PaymentMonthDto(d.getPaymentDate().getMonthValue(), d.getPaymentDate().getYear())
                        : new PaymentMonthDto(d.getMonth(), d.getYear()))
                .distinct()
                .sorted(java.util.Comparator
                        .comparingInt(PaymentMonthDto::year)
                        .thenComparingInt(PaymentMonthDto::month))
                .toList();

        // 실제 지급일 목록 (paymentDate 있는 row만, 오름차순)
        List<LocalDate> paymentDates = baseRows.stream()
                .filter(d -> d.getPaymentDate() != null)
                .map(Dividend::getPaymentDate)
                .distinct()
                .sorted()
                .toList();

        int dividendSum = baseRows.stream()
                .mapToInt(d -> {
                    if ("CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null)
                        return d.getConfirmedAmount().intValue();
                    return d.getExpectedAmount() != null ? d.getExpectedAmount().intValue() : 0;
                })
                .sum();

        // dividendCycle: DB 저장값 우선, 없을 때만 row 개수로 파생
        // (row가 일부만 생성된 경우 개수 기반 파생이 틀릴 수 있으므로
        //  DART에서 감지해 저장한 stock.getDividendCycle()을 신뢰)
        String storedCycle  = stock.getDividendCycle();
        String derivedCycle = deriveCycle(paymentMonths.size());
        String finalCycle   = storedCycle != null ? storedCycle : derivedCycle;

        // 연간 주당 배당금: DB 저장값 우선 (DART에서 설정한 연간 합계)
        // row 합산은 row가 일부만 생성된 경우 과소 계산되므로 폴백으로만 사용
        int storedPerShare = stock.getExpectedDividendPerShare() != null
                ? stock.getExpectedDividendPerShare() : 0;
        int rowPerShare = (dividendSum > 0 && netQty > 0)
                ? dividendSum / netQty : 0;
        int annualPerShare = storedPerShare > 0 ? storedPerShare : rowPerShare;

        return StockResponse.builder()
                .id(stock.getId())
                .userId(stock.getUser().getId())
                .stockName(stock.getStockName())
                .stockCode(stock.getStockCode())
                .sectorCode(sector != null ? sector.name() : null)
                .sectorLabel(sector != null ? sector.getLabel() : null)
                .exchange(stock.getExchange())
                .currency(stock.getCurrency())
                .quantity(netQty)
                .avgPrice(avgPx)
                .previousClose(close != null && close.compareTo(BigDecimal.ZERO) > 0 ? close : null)
                .evaluationAmount(evalAmount)
                .evaluationProfit(evalProfit)
                .profitRate(profitRate)
                .investmentWeight(weight)
                .paymentMonths(paymentMonths.isEmpty() ? null : paymentMonths)
                .paymentDates(paymentDates.isEmpty() ? null : paymentDates)
                .expectedDividend(dividendSum > 0 ? dividendSum : null)
                .dividendCycle(finalCycle)
                .expectedDividendPerShare(annualPerShare > 0 ? annualPerShare : null)
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }

    /** Dividend row 개수로 배당 주기 자동 판별 */
    private static String deriveCycle(int monthCount) {
        return switch (monthCount) {
            case 12 -> "MONTHLY";
            case 4  -> "QUARTERLY";
            case 2  -> "SEMI_ANNUAL";
            case 1  -> "ANNUAL";
            default -> null;
        };
    }
}
