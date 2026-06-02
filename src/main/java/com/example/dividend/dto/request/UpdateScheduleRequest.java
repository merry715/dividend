package com.example.dividend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateScheduleRequest {

    /** 대상 종목 ID */
    private Long stockId;

    /** 주당 배당금 (원) */
    private int dividendPerShare;

    /** 첫 번째 지급월 (1~12) */
    private int firstPaymentMonth;

    /** 배당 주기: QUARTERLY / SEMI_ANNUAL / ANNUAL / MONTHLY */
    private String dividendCycle;
}
