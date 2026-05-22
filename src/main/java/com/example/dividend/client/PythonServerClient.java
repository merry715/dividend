package com.example.dividend.client;

import com.example.dividend.dto.response.StockSearchResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PythonServerClient {

    private final RestClient restClient;

    public PythonServerClient(
            @Value("${stock.search.python-server-url:http://localhost:5000}") String baseUrl,
            @Value("${stock.search.timeout-seconds:3}") int timeoutSeconds) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /** 종목명 검색 — 실패 시 빈 목록 반환 */
    public List<StockSearchResult> searchStocks(String query) {
        try {
            StockSearchResult[] results = restClient.get()
                    .uri("/search?q={q}", query.trim())
                    .retrieve()
                    .body(StockSearchResult[].class);
            return results != null ? List.of(results) : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("종목 검색 서버 호출 실패 [query={}]: {}", query, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("종목 검색 중 오류 [query={}]: {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /** 전일 종가 조회 — 실패 시 null 반환 (기존 값 유지) */
    public BigDecimal fetchPrice(String ticker) {
        try {
            PriceResponse resp = restClient.get()
                    .uri("/internal/price?ticker={t}", ticker)
                    .retrieve()
                    .body(PriceResponse.class);
            return resp != null ? resp.getPrice() : null;
        } catch (RestClientException e) {
            log.warn("종가 조회 실패 [ticker={}]: {}", ticker, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("종가 조회 중 오류 [ticker={}]: {}", ticker, e.getMessage(), e);
            return null;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PriceResponse {
        private String ticker;
        private BigDecimal price;
    }
}
