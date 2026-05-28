package com.example.dividend.service;

import com.example.dividend.client.PythonServerClient;
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
 * 주가 조회 공통 fallback 모듈.
 *
 * 우선순위:
 *   1. yfinance (Python server)
 *   2. stock_price_cache 테이블 (마지막 성공 캐시)
 *   3. 해당 종목의 평균 매입단가 (avg_price)
 *   4. 0 반환 + source = "unavailable"
 *
 * 모든 주가 조회 로직은 이 서비스를 통해 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceFallbackService {

    private final PythonServerClient       pythonServerClient;
    private final StockPriceCacheRepository cacheRepository;

    /**
     * KRX 종목 전용 진입점.
     * exchange 미확인 종목은 .KS → .KQ 순으로 yfinance를 재시도하며,
     * 이후 DB 캐시 → 평균 매입단가 → unavailable 순서로 fallback한다.
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

        // exchange 미설정: .KS → .KQ yfinance 재시도 후 DB/avg fallback
        return fetchWithExchangeRetry(code, avg);
    }

    /**
     * 단일 ticker 기준 4단계 fallback.
     * yfinance 성공 시 stock_price_cache를 upsert한다.
     */
    @Transactional
    public PriceResult fetchPrice(String ticker, BigDecimal avgPrice) {
        // ① yfinance
        BigDecimal yfPrice = pythonServerClient.fetchPrice(ticker);
        if (yfPrice != null && yfPrice.compareTo(BigDecimal.ZERO) > 0) {
            upsertCache(ticker, yfPrice);
            return PriceResult.ofYfinance(yfPrice);
        }
        log.warn("yfinance 조회 실패 또는 데이터 없음 [ticker={}] — 캐시 조회", ticker);

        // ② DB 캐시
        Optional<StockPriceCache> cached = cacheRepository.findById(ticker);
        if (cached.isPresent()) {
            log.warn("캐시 가격 사용 [ticker={}, price={}]", ticker, cached.get().getPrice());
            return PriceResult.ofCache(cached.get().getPrice());
        }

        // ③ 평균 매입단가
        if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("평균 매입단가 사용 [ticker={}, avgPrice={}]", ticker, avgPrice);
            return PriceResult.ofAvgPurchase(avgPrice);
        }

        // ④ unavailable
        log.warn("가격 조회 완전 실패 [ticker={}] — 0 반환, source=unavailable", ticker);
        return PriceResult.unavailable();
    }

    // ── 내부 메서드 ─────────────────────────────────────────────────────────────

    /**
     * exchange 미확인 종목: .KS → .KQ 순으로 yfinance 재시도.
     * 둘 다 실패하면 캐시(.KS 우선) → avgPrice → unavailable.
     */
    private PriceResult fetchWithExchangeRetry(String code, BigDecimal avgPrice) {
        // yfinance .KS
        BigDecimal ksPrice = pythonServerClient.fetchPrice(code + ".KS");
        if (ksPrice != null && ksPrice.compareTo(BigDecimal.ZERO) > 0) {
            upsertCache(code + ".KS", ksPrice);
            return PriceResult.ofYfinance(ksPrice);
        }

        // yfinance .KQ
        log.info("KOSPI 조회 실패, KOSDAQ 재시도 [code={}]", code);
        BigDecimal kqPrice = pythonServerClient.fetchPrice(code + ".KQ");
        if (kqPrice != null && kqPrice.compareTo(BigDecimal.ZERO) > 0) {
            upsertCache(code + ".KQ", kqPrice);
            return PriceResult.ofYfinance(kqPrice);
        }

        log.warn("yfinance 조회 실패 [code={}] — 캐시 조회", code);

        // 캐시: .KS 우선, 없으면 .KQ
        Optional<StockPriceCache> cached = cacheRepository.findById(code + ".KS")
                .or(() -> cacheRepository.findById(code + ".KQ"));
        if (cached.isPresent()) {
            log.warn("캐시 가격 사용 [code={}, ticker={}, price={}]",
                    code, cached.get().getTicker(), cached.get().getPrice());
            return PriceResult.ofCache(cached.get().getPrice());
        }

        // 평균 매입단가
        if (avgPrice != null && avgPrice.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("평균 매입단가 사용 [code={}, avgPrice={}]", code, avgPrice);
            return PriceResult.ofAvgPurchase(avgPrice);
        }

        log.warn("가격 조회 완전 실패 [code={}] — 0 반환, source=unavailable", code);
        return PriceResult.unavailable();
    }

    private void upsertCache(String ticker, BigDecimal price) {
        StockPriceCache entry = cacheRepository.findById(ticker)
                .orElse(new StockPriceCache(ticker, price));
        entry.updatePrice(price);
        cacheRepository.save(entry);
    }
}
