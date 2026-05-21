package com.example.dividend.service;

import com.example.dividend.client.PythonServerClient;
import com.example.dividend.entity.Stock;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceUpdateService {

    private final StockRepository stockRepository;
    private final PythonServerClient pythonServerClient;

    /**
     * 매일 오후 6시 — 전체 종목 전일 종가 일괄 업데이트.
     * cron 식은 application.yml의 stock.schedule.price-update-cron으로 재정의 가능.
     */
    @Scheduled(cron = "${stock.schedule.price-update-cron:0 0 18 * * *}")
    @Transactional
    public void updateAllPreviousClose() {
        List<Stock> stocks = stockRepository.findAll();
        log.info("전일 종가 일괄 업데이트 시작: {}개 종목", stocks.size());

        int success = 0;
        int skipped = 0;

        for (Stock stock : stocks) {
            BigDecimal price = fetchPriceWithFallback(stock);

            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                stock.setPreviousClose(price);
                success++;
            } else {
                // 실패 시 기존 종가 유지 — warn 로그는 PythonServerClient에서 이미 기록
                skipped++;
            }
        }

        log.info("전일 종가 업데이트 완료: 성공={}, 유지={}", success, skipped);
    }

    /**
     * KRX 종목 전용: exchange 값으로 KOSPI(.KS) / KOSDAQ(.KQ) 구분.
     * exchange가 KOSDAQ이면 .KQ, 나머지(KOSPI·KRX·미설정)는 .KS 적용.
     */
    private String toTicker(Stock stock) {
        String code = stock.getStockCode();
        String exchange = stock.getExchange() != null ? stock.getExchange().toUpperCase() : "";
        return "KOSDAQ".equals(exchange) ? code + ".KQ" : code + ".KS";
    }

    /**
     * exchange 미확인 종목의 경우: .KS로 조회 실패 시 .KQ로 재시도.
     */
    private BigDecimal fetchPriceWithFallback(Stock stock) {
        String code = stock.getStockCode();
        String exchange = stock.getExchange() != null ? stock.getExchange().toUpperCase() : "";

        // exchange가 명확하면 단순 조회
        if ("KOSDAQ".equals(exchange) || "KOSPI".equals(exchange) || "KRX".equals(exchange)) {
            return pythonServerClient.fetchPrice(toTicker(stock));
        }

        // exchange 미설정: .KS 먼저 시도, 실패 시 .KQ 재시도
        BigDecimal price = pythonServerClient.fetchPrice(code + ".KS");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("KOSPI 조회 실패, KOSDAQ으로 재시도 [code={}]", code);
            price = pythonServerClient.fetchPrice(code + ".KQ");
        }
        return price;
    }
}
