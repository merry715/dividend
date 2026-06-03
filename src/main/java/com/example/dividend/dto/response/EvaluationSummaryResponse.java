package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class EvaluationSummaryResponse {

    /** 전체 보유 종목 수 */
    private int totalStocks;

    /** 종가가 있어 계산에 포함된 종목 수 */
    private int priceAvailableStocks;

    /** 총 투자 원금 (종가 있는 종목만) */
    private BigDecimal totalInvestment;

    /** 총 평가 금액 */
    private BigDecimal totalEvaluation;

    /** 총 평가 손익 = 총 평가 금액 - 총 투자 원금 */
    private BigDecimal totalGain;

    /** 총 수익률(%), 소수점 2자리. 투자 원금 0이면 null */
    private BigDecimal totalReturnRate;

    /**
     * 종가 데이터 출처별 종목 수.
     * 예: { "yfinance": 3, "cache": 1, "avg_purchase": 1, "unavailable": 2 }
     */
    private Map<String, Long> priceSourceCounts;
}
