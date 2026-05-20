package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ExpectedDividendResponse {

    private Long stockId;
    private String stockCode;
    private String stockName;
    private String currency;
    private int quantity;

    /** 배당 항목 목록 (연도 내림차순, 지급월 오름차순) */
    private List<DividendItem> dividends;

    /** 연도별 예상 수령 합산 */
    private List<AnnualSummary> annualSummary;

    @Getter
    @Builder
    public static class DividendItem {
        private Long id;
        private int year;
        private int paymentMonth;
        private LocalDate exDividendDate;
        private LocalDate paymentDate;
        private String status;
        /** CONFIRMED이면 confirmedDividend, EXPECTED이면 expectedDividend */
        private int perShareDividend;
        /** perShareDividend × 보유수량 */
        private int estimatedReceive;
    }

    @Getter
    @Builder
    public static class AnnualSummary {
        private int year;
        private int totalEstimatedReceive;
    }
}
