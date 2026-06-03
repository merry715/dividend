package com.example.dividend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DividendUpdateRequest {

    /** 주당 배당금 */
    private Long amount;

    /** 배당락일 (사용자 입력값, null 허용) */
    private LocalDate exDividendDate;

    /** 지급일 */
    private LocalDate paymentDate;

    /** 상태: CONFIRMED | EXPECTED (없으면 EXPECTED 유지) */
    private String status;
}
