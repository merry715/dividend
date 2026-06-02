package com.example.dividend.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DividendConfirmRequest {

    /** 확정 배당금. 양수면 CONFIRMED, null/0이면 EXPECTED로 되돌림 */
    private Long confirmedAmount;

    /** 배당락일 (사용자 입력값, null 허용) */
    private LocalDate exDividendDate;

    private LocalDate paymentDate;
    // month 필드 제거 — 확정 전환 시 지급월 변경을 허용하면
    // monthly 집계가 어긋나는 버그가 재발할 수 있으므로 차단.
}
