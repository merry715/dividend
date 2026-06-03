package com.example.dividend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * yfinance 조회 성공 시 ticker별 마지막 종가를 저장하는 글로벌 캐시 테이블.
 * yfinance 실패 시 이 테이블에서 가장 최근 가격을 fallback으로 사용한다.
 */
@Entity
@Table(name = "stock_price_cache")
@Getter
@NoArgsConstructor
public class StockPriceCache {

    @Id
    @Column(length = 20)
    private String ticker;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public StockPriceCache(String ticker, BigDecimal price) {
        this.ticker = ticker;
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePrice(BigDecimal price) {
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }
}
