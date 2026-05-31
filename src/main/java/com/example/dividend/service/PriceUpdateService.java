package com.example.dividend.service;

import com.example.dividend.dto.PriceResult;
import com.example.dividend.entity.Stock;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceUpdateService {

    private final StockRepository            stockRepository;
    private final StockPriceFallbackService  fallbackService;

    /**
     * 매일 오후 6시 — 전체 종목 전일 종가 일괄 업데이트.
     * cron 식은 application.yml의 stock.schedule.price-update-cron으로 재정의 가능.
     */
    @Scheduled(cron = "${stock.schedule.price-update-cron:0 0 18 * * *}")
    @Transactional
    public void updateAllPreviousClose() {
        List<Stock> stocks = stockRepository.findAll();
        log.info("전일 종가 일괄 업데이트 시작: {}개 종목", stocks.size());

        int yfinance = 0, cache = 0, avgPurchase = 0, unavailable = 0;

        for (Stock stock : stocks) {
            PriceResult result = fallbackService.fetchPriceForStock(stock);

            stock.setPriceSource(result.getSource());

            // avg_purchase fallback은 현재가로 표시하지 않음 (평균단가=현재가 혼동 방지)
            boolean isRealPrice = PriceResult.SOURCE_YFINANCE.equals(result.getSource())
                    || PriceResult.SOURCE_CACHE.equals(result.getSource());
            if (isRealPrice && result.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                stock.setPreviousClose(result.getPrice());
            }
            // unavailable이어도 priceSource는 기록 (기존 previousClose 유지)

            switch (result.getSource()) {
                case PriceResult.SOURCE_YFINANCE     -> yfinance++;
                case PriceResult.SOURCE_CACHE        -> cache++;
                case PriceResult.SOURCE_AVG_PURCHASE -> avgPurchase++;
                default                              -> unavailable++;
            }
        }

        log.info("전일 종가 업데이트 완료: yfinance={}, cache={}, avg_purchase={}, unavailable={}",
                yfinance, cache, avgPurchase, unavailable);
    }
}
