package com.example.dividend.dto;

import java.math.BigDecimal;

/**
 * 주가 조회 결과 — 가격 값과 데이터 출처를 함께 전달한다.
 * source 값: "yfinance" | "cache" | "avg_purchase" | "unavailable"
 */
public class PriceResult {

    public static final String SOURCE_YFINANCE      = "yfinance";
    public static final String SOURCE_CACHE         = "cache";
    public static final String SOURCE_AVG_PURCHASE  = "avg_purchase";
    public static final String SOURCE_UNAVAILABLE   = "unavailable";

    private final BigDecimal price;
    private final String source;

    private PriceResult(BigDecimal price, String source) {
        this.price  = price;
        this.source = source;
    }

    public static PriceResult ofYfinance(BigDecimal price) {
        return new PriceResult(price, SOURCE_YFINANCE);
    }

    public static PriceResult ofCache(BigDecimal price) {
        return new PriceResult(price, SOURCE_CACHE);
    }

    public static PriceResult ofAvgPurchase(BigDecimal price) {
        return new PriceResult(price, SOURCE_AVG_PURCHASE);
    }

    public static PriceResult unavailable() {
        return new PriceResult(BigDecimal.ZERO, SOURCE_UNAVAILABLE);
    }

    public BigDecimal getPrice()  { return price; }
    public String    getSource()  { return source; }

    public boolean isPriceAvailable() {
        return !SOURCE_UNAVAILABLE.equals(source) && price.compareTo(BigDecimal.ZERO) > 0;
    }
}
