package com.example.dividend.service;

import com.example.dividend.dto.response.SectorAnalysisResponse;
import com.example.dividend.dto.response.SectorAnalysisResponse.SectorItem;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.StockSector;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectorService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final StockRepository stockRepository;

    public SectorAnalysisResponse getSectorAnalysis(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_Id(userId);

        // 섹터별 종목 수·평가 금액 집계 (enum 선언 순서 유지)
        Map<StockSector, Integer>    countBySector = new LinkedHashMap<>();
        Map<StockSector, BigDecimal> evalBySector  = new LinkedHashMap<>();
        for (StockSector s : StockSector.values()) {
            countBySector.put(s, 0);
            evalBySector.put(s, BigDecimal.ZERO);
        }

        int unclassifiedCount          = 0;
        BigDecimal unclassifiedEval    = BigDecimal.ZERO;
        BigDecimal totalEvaluation     = BigDecimal.ZERO;

        for (Stock stock : stocks) {
            BigDecimal eval = evaluationOf(stock);
            totalEvaluation = totalEvaluation.add(eval);

            StockSector sector = stock.getSector();
            if (sector != null) {
                countBySector.merge(sector, 1, Integer::sum);
                evalBySector.merge(sector, eval, BigDecimal::add);
            } else {
                unclassifiedCount++;
                unclassifiedEval = unclassifiedEval.add(eval);
            }
        }

        // 종목이 있는 섹터만 포함, 평가 금액 내림차순 정렬
        List<SectorItem> items = new ArrayList<>();
        for (StockSector sector : StockSector.values()) {
            int count = countBySector.get(sector);
            if (count == 0) continue;

            BigDecimal eval = evalBySector.get(sector);
            items.add(SectorItem.builder()
                    .sectorCode(sector.name())
                    .sectorLabel(sector.getLabel())
                    .stockCount(count)
                    .totalEvaluation(eval)
                    .weight(calcWeight(eval, totalEvaluation))
                    .build());
        }
        items.sort(Comparator.comparing(SectorItem::getTotalEvaluation).reversed());

        // 미분류 종목은 항상 마지막에 추가
        if (unclassifiedCount > 0) {
            items.add(SectorItem.builder()
                    .sectorCode(null)
                    .sectorLabel("미분류")
                    .stockCount(unclassifiedCount)
                    .totalEvaluation(unclassifiedEval)
                    .weight(calcWeight(unclassifiedEval, totalEvaluation))
                    .build());
        }

        return SectorAnalysisResponse.builder()
                .totalStocks(stocks.size())
                .totalEvaluation(totalEvaluation)
                .sectors(items)
                .build();
    }

    private BigDecimal evaluationOf(Stock stock) {
        BigDecimal close = stock.getPreviousClose();
        if (close == null || close.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return close.multiply(BigDecimal.valueOf(stock.getQuantity()));
    }

    private BigDecimal calcWeight(BigDecimal sectorEval, BigDecimal totalEval) {
        if (totalEval.compareTo(BigDecimal.ZERO) == 0) return null;
        return sectorEval.divide(totalEval, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
