package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EvaluationResponse {

    private Long stockId;
    private String stockCode;
    private String stockName;
    private String currency;
    private int quantity;
    private BigDecimal avgPrice;

    /** 전일 종가. 아직 없으면 null */
    private BigDecimal previousClose;

    /** 평가 금액 = 보유 수량 × 전일 종가. 종가 없으면 null */
    private BigDecimal evaluationAmount;

    /** 평가 손익 = 평가 금액 - 투자 원금. 종가 없으면 null */
    private BigDecimal evaluationGain;

    /** 수익률(%), 소수점 2자리. 투자 원금 0이거나 종가 없으면 null */
    private BigDecimal returnRate;
}
