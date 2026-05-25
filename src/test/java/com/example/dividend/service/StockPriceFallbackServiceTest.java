package com.example.dividend.service;

import com.example.dividend.client.PythonServerClient;
import com.example.dividend.dto.PriceResult;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.StockPriceCache;
import com.example.dividend.repository.StockPriceCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockPriceFallbackServiceTest {

    @Mock PythonServerClient        pythonServerClient;
    @Mock StockPriceCacheRepository cacheRepository;

    @InjectMocks StockPriceFallbackService fallbackService;

    // ── fetchPrice (단일 ticker) ─────────────────────────────────────────────

    @Test
    @DisplayName("① yfinance 성공 → source=yfinance, 캐시 upsert")
    void fetchPrice_yfinanceSuccess() {
        given(pythonServerClient.fetchPrice("005930.KS"))
                .willReturn(new BigDecimal("71000"));
        given(cacheRepository.findById("005930.KS"))
                .willReturn(Optional.empty());

        PriceResult result = fallbackService.fetchPrice("005930.KS", BigDecimal.ZERO);

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_YFINANCE);
        assertThat(result.getPrice()).isEqualByComparingTo("71000");
        assertThat(result.isPriceAvailable()).isTrue();

        ArgumentCaptor<StockPriceCache> captor = ArgumentCaptor.forClass(StockPriceCache.class);
        verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().getTicker()).isEqualTo("005930.KS");
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("71000");
    }

    @Test
    @DisplayName("② yfinance 실패, 캐시 있음 → source=cache")
    void fetchPrice_yfinanceFail_cacheHit() {
        given(pythonServerClient.fetchPrice("005930.KS")).willReturn(null);

        StockPriceCache cached = new StockPriceCache("005930.KS", new BigDecimal("70000"));
        given(cacheRepository.findById("005930.KS")).willReturn(Optional.of(cached));

        PriceResult result = fallbackService.fetchPrice("005930.KS", new BigDecimal("65000"));

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_CACHE);
        assertThat(result.getPrice()).isEqualByComparingTo("70000");
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("③ yfinance 실패, 캐시 없음, avgPrice 있음 → source=avg_purchase")
    void fetchPrice_yfinanceFail_noCache_hasAvg() {
        given(pythonServerClient.fetchPrice("005930.KS")).willReturn(null);
        given(cacheRepository.findById("005930.KS")).willReturn(Optional.empty());

        PriceResult result = fallbackService.fetchPrice("005930.KS", new BigDecimal("65000"));

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_AVG_PURCHASE);
        assertThat(result.getPrice()).isEqualByComparingTo("65000");
    }

    @Test
    @DisplayName("④ yfinance 실패, 캐시 없음, avgPrice 없음 → source=unavailable, price=0")
    void fetchPrice_allFail_unavailable() {
        given(pythonServerClient.fetchPrice("005930.KS")).willReturn(null);
        given(cacheRepository.findById("005930.KS")).willReturn(Optional.empty());

        PriceResult result = fallbackService.fetchPrice("005930.KS", BigDecimal.ZERO);

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_UNAVAILABLE);
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.isPriceAvailable()).isFalse();
    }

    @Test
    @DisplayName("yfinance 0 반환 → 유효하지 않은 값으로 처리, cache로 fallback")
    void fetchPrice_yfinanceReturnsZero_fallbackToCache() {
        given(pythonServerClient.fetchPrice("005930.KS")).willReturn(BigDecimal.ZERO);

        StockPriceCache cached = new StockPriceCache("005930.KS", new BigDecimal("70000"));
        given(cacheRepository.findById("005930.KS")).willReturn(Optional.of(cached));

        PriceResult result = fallbackService.fetchPrice("005930.KS", new BigDecimal("65000"));

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_CACHE);
    }

    // ── fetchPriceForStock (KRX 종목, exchange 포함) ─────────────────────────

    @Test
    @DisplayName("KOSPI 종목: .KS 티커로 조회")
    void fetchPriceForStock_kospi_usesKsTicker() {
        Stock stock = buildStock("005930", "KOSPI", new BigDecimal("65000"));
        given(pythonServerClient.fetchPrice("005930.KS"))
                .willReturn(new BigDecimal("71000"));
        given(cacheRepository.findById("005930.KS")).willReturn(Optional.empty());

        PriceResult result = fallbackService.fetchPriceForStock(stock);

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_YFINANCE);
        verify(pythonServerClient).fetchPrice("005930.KS");
        verify(pythonServerClient, never()).fetchPrice("005930.KQ");
    }

    @Test
    @DisplayName("KOSDAQ 종목: .KQ 티커로 조회")
    void fetchPriceForStock_kosdaq_usesKqTicker() {
        Stock stock = buildStock("035420", "KOSDAQ", new BigDecimal("80000"));
        given(pythonServerClient.fetchPrice("035420.KQ"))
                .willReturn(new BigDecimal("85000"));
        given(cacheRepository.findById("035420.KQ")).willReturn(Optional.empty());

        PriceResult result = fallbackService.fetchPriceForStock(stock);

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_YFINANCE);
        verify(pythonServerClient).fetchPrice("035420.KQ");
        verify(pythonServerClient, never()).fetchPrice("035420.KS");
    }

    @Test
    @DisplayName("exchange 미설정 종목: .KS 성공 시 .KQ 시도 안 함")
    void fetchPriceForStock_unknownExchange_ksSucceeds() {
        Stock stock = buildStock("005930", null, new BigDecimal("65000"));
        given(pythonServerClient.fetchPrice("005930.KS"))
                .willReturn(new BigDecimal("71000"));
        given(cacheRepository.findById("005930.KS")).willReturn(Optional.empty());

        PriceResult result = fallbackService.fetchPriceForStock(stock);

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_YFINANCE);
        verify(pythonServerClient, never()).fetchPrice("005930.KQ");
    }

    @Test
    @DisplayName("exchange 미설정: .KS 실패 → .KQ yfinance 성공")
    void fetchPriceForStock_unknownExchange_ksFail_kqSuccess() {
        Stock stock = buildStock("035420", null, new BigDecimal("80000"));
        given(pythonServerClient.fetchPrice("035420.KS")).willReturn(null);
        given(pythonServerClient.fetchPrice("035420.KQ"))
                .willReturn(new BigDecimal("85000"));
        given(cacheRepository.findById("035420.KQ")).willReturn(Optional.empty());

        PriceResult result = fallbackService.fetchPriceForStock(stock);

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_YFINANCE);
        assertThat(result.getPrice()).isEqualByComparingTo("85000");
    }

    @Test
    @DisplayName("exchange 미설정: yfinance 둘 다 실패, .KS 캐시 사용")
    void fetchPriceForStock_unknownExchange_bothFail_ksCache() {
        Stock stock = buildStock("005930", null, BigDecimal.ZERO);
        given(pythonServerClient.fetchPrice("005930.KS")).willReturn(null);
        given(pythonServerClient.fetchPrice("005930.KQ")).willReturn(null);

        StockPriceCache cached = new StockPriceCache("005930.KS", new BigDecimal("70000"));
        given(cacheRepository.findById("005930.KS")).willReturn(Optional.of(cached));

        PriceResult result = fallbackService.fetchPriceForStock(stock);

        assertThat(result.getSource()).isEqualTo(PriceResult.SOURCE_CACHE);
        assertThat(result.getPrice()).isEqualByComparingTo("70000");
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Stock buildStock(String code, String exchange, BigDecimal avgPrice) {
        Stock stock = new Stock();
        stock.setStockCode(code);
        stock.setExchange(exchange);
        stock.setAvgPrice(avgPrice);
        return stock;
    }
}
