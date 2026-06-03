package com.example.dividend.client;

import lombok.Getter;

import java.time.LocalDate;

/**
 * 공공데이터포털 주식배당정보 단일 레코드.
 */
@Getter
public class DataGoKrDividendInfo {

    /** 현금배당지급일자 (null 가능) */
    private final LocalDate payDate;

    /** 주당배당금액 (원) */
    private final int amountPerShare;

    /** 배당기준일 (dvdnBasDt) ← 주기 판별에 사용 */
    private final LocalDate baseDt;

    /** 기존 호환 생성자 (baseDt 없음) */
    public DataGoKrDividendInfo(LocalDate payDate, int amountPerShare) {
        this.payDate       = payDate;
        this.amountPerShare = amountPerShare;
        this.baseDt        = null;
    }

    /** baseDt 포함 생성자 */
    public DataGoKrDividendInfo(LocalDate payDate, int amountPerShare, LocalDate baseDt) {
        this.payDate        = payDate;
        this.amountPerShare = amountPerShare;
        this.baseDt         = baseDt;
    }

    /** payDate 기준 지급 월 (null이면 0) */
    public int getMonth() {
        return payDate != null ? payDate.getMonthValue() : 0;
    }

    /** baseDt 기준 배당기준 월 (null이면 0) */
    public int getBaseDtMonth() {
        return baseDt != null ? baseDt.getMonthValue() : 0;
    }
}
