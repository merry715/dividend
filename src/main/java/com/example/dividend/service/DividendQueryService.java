package com.example.dividend.service;

import com.example.dividend.dto.response.ExpectedDividendResponse;
import com.example.dividend.dto.response.ExpectedDividendResponse.AnnualSummary;
import com.example.dividend.dto.response.ExpectedDividendResponse.DividendItem;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.exception.AccessForbiddenException;
import com.example.dividend.exception.StockNotFoundException;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DividendQueryService {

    private final StockRepository stockRepository;
    private final DividendRepository dividendRepository;

    public ExpectedDividendResponse getExpectedDividends(Long stockId, Long userId) {
        // fetch join으로 Stock + User를 단일 쿼리에 조회
        Stock stock = stockRepository.findByIdWithUser(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));

        if (!stock.getUser().getId().equals(userId)) {
            throw new AccessForbiddenException("해당 종목에 접근할 권한이 없습니다");
        }

        List<Dividend> dividends =
                dividendRepository.findByStockIdOrderByYearDescPaymentMonthAsc(stockId);

        List<DividendItem> items = dividends.stream()
                .map(d -> toItem(d, stock.getQuantity()))
                .toList();

        List<AnnualSummary> annualSummary = buildAnnualSummary(items);

        return ExpectedDividendResponse.builder()
                .stockId(stock.getId())
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .currency(stock.getCurrency())
                .quantity(stock.getQuantity())
                .dividends(items)
                .annualSummary(annualSummary)
                .build();
    }

    private DividendItem toItem(Dividend d, int quantity) {
        long totalAmount = "CONFIRMED".equals(d.getStatus()) && d.getConfirmedAmount() != null
                ? d.getConfirmedAmount().longValue()
                : (d.getExpectedAmount() != null ? d.getExpectedAmount().longValue() : 0L);
        int perShare = quantity > 0 ? (int) (totalAmount / quantity) : 0;

        return DividendItem.builder()
                .id(d.getId())
                .year(d.getYear())
                .paymentMonth(d.getMonth())
                .exDividendDate(d.getExDividendDate())
                .paymentDate(d.getPaymentDate())
                .status(d.getStatus())
                .perShareDividend(perShare)
                .estimatedReceive((int) totalAmount)
                .build();
    }

    private List<AnnualSummary> buildAnnualSummary(List<DividendItem> items) {
        // 삽입 순서(연도 내림차순)를 유지하며 연도별 합산
        Map<Integer, Integer> byYear = new LinkedHashMap<>();
        for (DividendItem item : items) {
            byYear.merge(item.getYear(), item.getEstimatedReceive(), Integer::sum);
        }
        return byYear.entrySet().stream()
                .map(e -> AnnualSummary.builder()
                        .year(e.getKey())
                        .totalEstimatedReceive(e.getValue())
                        .build())
                .toList();
    }
}
