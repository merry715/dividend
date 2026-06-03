package com.example.dividend.service;

import com.example.dividend.client.DataGoKrDividendClient;
import com.example.dividend.client.DataGoKrDividendInfo;
import com.example.dividend.entity.Stock;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 연간 배당 데이터 갱신 배치.
 * 매년 1월 1일 자정에 전체 종목 배당 row를 data.go.kr 실데이터로 갱신.
 * dividend.scheduler.enabled=false 로 비활성화 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DividendBatchService {

    @Value("${dividend.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    private final StockRepository        stockRepository;
    private final DataGoKrDividendClient dataGoKrClient;
    private final DividendService        dividendService;

    @Scheduled(cron = "0 0 0 1 1 *")
    public void updateAllDividends() {
        if (!schedulerEnabled) {
            log.info("[배당 배치] 스케줄러 비활성화 — 건너뜀");
            return;
        }

        int year = LocalDate.now().getYear();
        log.info("[배당 배치] 연간 배당 데이터 갱신 시작 [year={}]", year);

        List<Stock> stocks = stockRepository.findAll();
        int success = 0, skip = 0, fail = 0;

        for (Stock stock : stocks) {
            try {
                Long userId = stock.getUser().getId();

                List<DataGoKrDividendInfo> apiRecords =
                        dataGoKrClient.fetchAllDividendRecords(stock.getStockCode(), stock.getStockName(), year - 1);
                List<DataGoKrDividendInfo> validRecords = apiRecords.stream()
                        .filter(r -> r.getPayDate() != null)
                        .sorted(Comparator.comparing(r -> r.getPayDate().getMonthValue()))
                        .collect(Collectors.toList());

                if (validRecords.isEmpty()) {
                    log.info("[배당 배치] API 데이터 없음 — 건너뜀 [stockCode={}]", stock.getStockCode());
                    skip++;
                    Thread.sleep(1000); // 부하 분산
                    continue;
                }

                String cycle = dividendService.detectCycle(validRecords.size());
                dividendService.generateFromApiData(userId, stock.getId(), year, validRecords, cycle);
                log.info("[배당 배치] 갱신 완료 [stockCode={}, records={}건]",
                        stock.getStockCode(), validRecords.size());
                success++;

                Thread.sleep(1000); // 종목당 1초 딜레이
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[배당 배치] 갱신 실패 [stockCode={}]: {}", stock.getStockCode(), e.getMessage());
                fail++;
            }
        }

        log.info("[배당 배치] 연간 배당 데이터 갱신 완료 — 성공: {}건, 데이터없음: {}건, 실패: {}건",
                success, skip, fail);
    }
}
