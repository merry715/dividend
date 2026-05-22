package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class SectorAnalysisResponse {

    /** 전체 보유 종목 수 */
    private int totalStocks;

    /** 종가 기반 전체 평가 금액 합산 */
    private BigDecimal totalEvaluation;

    /** 섹터별 분석 항목 (평가 금액 내림차순) */
    private List<SectorItem> sectors;

    @Getter
    @Builder
    public static class SectorItem {
        /** Enum 이름 (ENERGY 등). 미분류 종목은 null */
        private String sectorCode;
        /** 한글 레이블. 미분류 종목은 "미분류" */
        private String sectorLabel;
        /** 해당 섹터 보유 종목 수 */
        private int stockCount;
        /** 섹터 평가 금액 합산 (종가 없는 종목은 0으로 계산) */
        private BigDecimal totalEvaluation;
        /** 투자 비중(%) = 섹터 평가 금액 / 전체 평가 금액 × 100. 전체 평가 0이면 null */
        private BigDecimal weight;
    }
}
