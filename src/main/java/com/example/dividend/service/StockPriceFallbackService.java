package com.example.dividend.service;

import com.example.dividend.client.DartNaverClient;
import com.example.dividend.dto.PriceResult;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.StockPriceCache;
import com.example.dividend.repository.StockPriceCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 주가 조회 공통 fallback 서비스.
 *
 * 우선순위:
 *   1. 네이버 금융 (실시간 현재가)
 *   2. stock_price_cache 테이블 (최근 성공 캐시)
 *   3. 해당 종목의 평균 매입가 (avg_price)
 *   4. 0 반환 + source = "unavailable"
 *
 * 모든 주가 조회 로직은 이 서비스를 거쳐 처리됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceFallbackService {

    private final DartNaverClient            DartNaverClient;
    private final StockPriceCacheRepository  cacheRepository;

    /**
     * KRX 종목 적용 진입점.
     * exchange 값으로 종목의 .KS 또는 .KQ 티커를 특정해 네이버 금융을 호출하고
     * 이후 DB 캐시 → 평균 매입가 → unavailable 로 fallback합니다.
     */
    @Transactional
    public PriceResult fetchPriceForStock(Stock stock) {
        String code     = stock.getStockCode();
        String exchange = stock.getExchange() != null ? stock.getExchange().toUpperCase() : "";
        BigDecimal avg  = stock.getAvgPrice();

        if ("KOSDAQ".equals(exchange)) {
            return fetchPrice(code + ".KQ", avg);
        }
        if ("KOSPI".equals(exchange) || "KRX".equals(exchange)) {
            return fetchPrice(code + ".KS", avg);
        }

        // exchange 미설정: .KS → .KQ 네이버 금융 재시도 후 DB/avg fallback
        return fetchWithExchangeRetry(code, avg);
    }

    /**
     * 단일 ticker 기준 4단계 fallback.
     * 네이버 금융 성공 시 stock_price_cache 를 upsert합니다.
     */
    @Transactional
    public PriceResult fetchPrice(String ticker, BigDecimal avgPrice) {
        // 1단계: 네이버 금융
        BigDecimal yfPrice = DartNaverClient.fetchPrice(ticker);
        if (yfPrice != null && yfPrice.compareTo(BigDecimal.ZERO) > 0) {
            upsertCache(ticker, yfPrice);
            return PriceResult.ofYfinance(yfPrice);
        }
        log.warn("네이버 금융 조회 실패 또는 데이터 없음 [ticker={}] → 캐시 조회", ticker);

        // 2단계: DB 캐시
        Optional<StockPriceCache> cached = cacheRepository.findById(ticker);
        if (cached.isPresent()) {
            log.warn("캐시 가격 사용 [ticker={}, price={}]", ticker, cached.get().getPrice());
            return PriceResult.ofCache(cached.get().getPrice());
        }

        // 3단계: 평균 매입가
        if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("평균 매입가 사용 [ticker={}, avgPrice={}]", ticker, avgPrice);
            return PriceResult.ofAvgPurchase(avgPrice);
        }

        // 4단계: unavailable
        log.warn("가격 조회 전부 실패 [ticker={}] → 0 반환, source=unavailable", ticker);
        return PriceResult.unavailable();
    }

    // ── 내부 메서드 ───────────────────────────────────────────────────────────

    /**
     * exchange 미설정 종목: .KS → .KQ 순으로 네이버 금융 재시도.
     * 모두 실패하면 캐시(.KS 우선) → avgPrice → unavailable.
     */
    private PriceResult fetchWithExchangeRetry(String code, BigDecimal avgPrice) {
        // 네이버 금융 .KS
        BigDecimal ksPrice = DartNaverClient.fetchPrice(code + ".KS");
        if (ksPrice != null && ksPrice.compareTo(BigDecimal.ZERO) > 0) {
            upsertCache(code + ".KS", ksPrice);
            return PriceResult.ofYfinance(ksPrice);
        }

        // 네이버 금융 .KQ
        log.info("KOSPI 조회 실패, KOSDAQ 재시도 [code={}]", code);
        BigDecimal kqPrice = DartNaverClient.fetchPrice(code + ".KQ");
        if (kqPrice != null && kqPrice.compareTo(BigDecimal.ZERO) > 0) {
            upsertCache(code + ".KQ", kqPrice);
            return PriceResult.ofYfinance(kqPrice);
        }

        log.warn("네이버 금융 조회 실패 [code={}] → 캐시 조회", code);

        // 캐시: .KS 우선, 없으면 .KQ
        Optional<StockPriceCache> cached = cacheRepository.findById(code + ".KS")
                .or(() -> cacheRepository.findById(code + ".KQ"));
        if (cached.isPresent()) {
            log.warn("캐시 가격 사용 [code={}, ticker={}, price={}]",
                    code, cached.get().getTicker(), cached.get().getPrice());
            return PriceResult.ofCache(cached.get().getPrice());
        }

        // 평균 매입가
        if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("평균 매입가 사용 [code={}, avgPrice={}]", code, avgPrice);
            return PriceResult.ofAvgPurchase(avgPrice);
        }

        log.warn("가격 조회 전부 실패 [code={}] → 0 반환, source=unavailable", code);
        return PriceResult.unavailable();
    }

    private void upsertCache(String ticker, BigDecimal price) {
        StockPriceCache entry = cacheRepository.findById(ticker)
                .orElse(new StockPriceCache(ticker, price));
        entry.updatePrice(price);
        cacheRepository.save(entry);
    }
}
