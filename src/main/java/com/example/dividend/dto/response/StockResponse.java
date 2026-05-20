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
    /** 올해 배당 지급월 목록 (정렬됨) */
    private List<Integer> paymentMonths;
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
    public static StockResponse from(Stock stock) {
        return of(stock, List.of(), BigDecimal.ZERO);
    }

    // ── 전체 조회용 (투자비중·배당 포함) ──────────────
    public static StockResponse of(Stock stock,
                                   List<Dividend> dividends,
                                   BigDecimal totalInvestment) {
        StockSector sector = stock.getSector();
        BigDecimal qty = BigDecimal.valueOf(stock.getQuantity());
        BigDecimal investment = stock.getAvgPrice().multiply(qty);

        // 평가 계산
        BigDecimal evalAmount = null, evalProfit = null, profitRate = null;
        BigDecimal close = stock.getPreviousClose();
        if (close != null && close.compareTo(BigDecimal.ZERO) > 0) {
            evalAmount = close.multiply(qty);
            evalProfit = evalAmount.subtract(investment);
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

        // 올해 배당 집계
        int currentYear = LocalDate.now().getYear();
        List<Dividend> thisYear = dividends.stream()
                .filter(d -> d.getYear() == currentYear)
                .toList();

        List<Integer> paymentMonths = thisYear.stream()
                .map(Dividend::getPaymentMonth)
                .distinct()
                .sorted()
                .toList();

        int dividendSum = thisYear.stream()
                .mapToInt(d -> "CONFIRMED".equals(d.getStatus())
                        ? d.getConfirmedDividend()
                        : d.getExpectedDividend())
                .sum();

        return StockResponse.builder()
                .id(stock.getId())
                .userId(stock.getUser().getId())
                .stockName(stock.getStockName())
                .stockCode(stock.getStockCode())
                .sectorCode(sector != null ? sector.name() : null)
                .sectorLabel(sector != null ? sector.getLabel() : null)
                .exchange(stock.getExchange())
                .currency(stock.getCurrency())
                .quantity(stock.getQuantity())
                .avgPrice(stock.getAvgPrice())
                .previousClose(close != null && close.compareTo(BigDecimal.ZERO) > 0 ? close : null)
                .evaluationAmount(evalAmount)
                .evaluationProfit(evalProfit)
                .profitRate(profitRate)
                .investmentWeight(weight)
                .paymentMonths(paymentMonths.isEmpty() ? null : paymentMonths)
                .expectedDividend(dividendSum > 0 ? dividendSum : null)
                .dividendCycle(stock.getDividendCycle())
                .expectedDividendPerShare(stock.getExpectedDividendPerShare())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}
