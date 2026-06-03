package com.example.dividend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DividendAutoGenerateRequest {

    /** 종목 ID */
    private Long stockId;

    /** 확정할 분기의 지급일 */
    private LocalDate firstPaymentDate;

    /** 사용자 입력 배당락일 (null 허용) */
    private LocalDate exDividendDate;

    /** 주당 배당금 */
    private Long dividendAmount;

    /** 배당주기: QUARTERLY | SEMI_ANNUAL (더 이상 자동생성에 사용하지 않음, 호환용 유지) */
    private String dividendCycle;
}
