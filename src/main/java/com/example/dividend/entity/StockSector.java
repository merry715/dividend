package com.example.dividend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockSector {

    ENERGY("에너지"),
    MATERIALS("소재"),
    INDUSTRIALS("산업재"),
    CONSUMER_DISCRETIONARY("경기소비재"),
    CONSUMER_STAPLES("필수소비재"),
    HEALTHCARE("헬스케어"),
    FINANCIALS("금융"),
    IT("IT"),
    COMMUNICATION("커뮤니케이션"),
    UTILITIES("유틸리티"),
    REAL_ESTATE("부동산");

    private final String label;
}
