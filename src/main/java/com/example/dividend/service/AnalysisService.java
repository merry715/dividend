package com.example.dividend.service;

import com.example.dividend.dto.response.*;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.StockSector;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final StockRepository    stockRepository;
    private final GoalRepository     goalRepository;
    private final HoldingService     holdingService;
    private final DividendService    dividendService;

    public GoalAchievementResponse getGoalAchievement(Long userId) {
        int currentYear = LocalDate.now().getYear();

        int targetDividend = goalRepository.findByUserIdAndYear(userId, currentYear)
                .map(g -> g.getTargetDividend())
                .orElse(0);

        int currentDividend = calcAnnualDividend(userId, currentYear);

        double achievementRate = targetDividend > 0
                ? Math.min((double) currentDividend / targetDividend * 100, 100.0)
                : 0.0;

        Integer estimatedMonths = null;
        if (targetDividend > 0 && currentDividend > 0 && currentDividend < targetDividend) {
            int remaining = targetDividend - currentDividend;
            double monthlyAverage = (double) currentDividend / 12;
            estimatedMonths = (int) Math.ceil(remaining / monthlyAverage);
        } else if (currentDividend >= targetDividend && targetDividend > 0) {
            estimatedMonths = 0;
        }

        return GoalAchievementResponse.builder()
                .targetDividend(targetDividend)
                .currentDividend(currentDividend)
                .achievementRate(Math.round(achievementRate * 10.0) / 10.0)
                .estimatedMonthsToGoal(estimatedMonths)
                .build();
    }

    public AnalysisSummaryResponse getSummary(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_Id(userId);
        Map<Long, HoldingService.HoldingInfo> holdingMap = holdingService.getHoldingMap(userId);

        BigDecimal totalInvestment = stocks.stream()
                .map(s -> {
                    HoldingService.HoldingInfo h = holdingMap.getOrDefault(
                            s.getId(), HoldingService.HoldingInfo.ZERO);
                    return h.avgPrice().multiply(BigDecimal.valueOf(Math.max(h.netQuantity(), 0)));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int currentYear = LocalDate.now().getYear();
        int totalExpectedDividend = calcAnnualDividend(userId, currentYear);

        return AnalysisSummaryResponse.builder()
                .totalInvestment(totalInvestment)
                .totalExpectedDividend(totalExpectedDividend)
                .stockCount((int) stocks.stream()
                        .filter(s -> holdingMap.getOrDefault(s.getId(), HoldingService.HoldingInfo.ZERO).netQuantity() > 0)
                        .count())
                .build();
    }

    public List<AnnualDividendResponse> getAnnualDividends(Long userId) {
        return dividendService.getYearly(userId).stream()
                .map(m -> AnnualDividendResponse.builder()
                        .year((Integer) m.get("year"))
                        .totalDividend(((Number) m.get("expectedAmount")).intValue())
                        .build())
                .toList();
    }

    public List<StockWeightResponse> getStockWeights(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_Id(userId);
        Map<Long, HoldingService.HoldingInfo> holdingMap = holdingService.getHoldingMap(userId);

        BigDecimal totalInvestment = stocks.stream()
                .map(s -> {
                    HoldingService.HoldingInfo h = holdingMap.getOrDefault(
                            s.getId(), HoldingService.HoldingInfo.ZERO);
                    return h.avgPrice().multiply(BigDecimal.valueOf(Math.max(h.netQuantity(), 0)));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return stocks.stream()
                .filter(s -> holdingMap.getOrDefault(s.getId(), HoldingService.HoldingInfo.ZERO).netQuantity() > 0)
                .map(s -> {
                    HoldingService.HoldingInfo h = holdingMap.getOrDefault(
                            s.getId(), HoldingService.HoldingInfo.ZERO);
                    BigDecimal investment = h.avgPrice().multiply(
                            BigDecimal.valueOf(Math.max(h.netQuantity(), 0)));
                    BigDecimal weight = totalInvestment.compareTo(BigDecimal.ZERO) > 0
                            ? investment.divide(totalInvestment, 6, RoundingMode.HALF_UP)
                                    .multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return StockWeightResponse.builder()
                            .stockId(s.getId())
                            .stockName(s.getStockName())
                            .stockCode(s.getStockCode())
                            .investment(investment)
                            .weightPercent(weight)
                            .build();
                })
                .sorted(Comparator.comparing(StockWeightResponse::getWeightPercent).reversed())
                .toList();
    }

    public List<SectorWeightResponse> getSectorWeights(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_Id(userId);
        Map<Long, HoldingService.HoldingInfo> holdingMap = holdingService.getHoldingMap(userId);

        BigDecimal totalInvestment = stocks.stream()
                .map(s -> {
                    HoldingService.HoldingInfo h = holdingMap.getOrDefault(
                            s.getId(), HoldingService.HoldingInfo.ZERO);
                    return h.avgPrice().multiply(BigDecimal.valueOf(Math.max(h.netQuantity(), 0)));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<StockSector, BigDecimal> bySector = new LinkedHashMap<>();
        for (Stock s : stocks) {
            HoldingService.HoldingInfo h = holdingMap.getOrDefault(
                    s.getId(), HoldingService.HoldingInfo.ZERO);
            if (h.netQuantity() <= 0) continue;
            BigDecimal investment = h.avgPrice().multiply(
                    BigDecimal.valueOf(h.netQuantity()));
            StockSector sector = s.getSector();
            bySector.merge(sector, investment, BigDecimal::add);
        }

        return bySector.entrySet().stream()
                .map(e -> {
                    BigDecimal weight = totalInvestment.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().divide(totalInvestment, 6, RoundingMode.HALF_UP)
                                    .multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    StockSector sector = e.getKey();
                    return SectorWeightResponse.builder()
                            .sectorCode(sector != null ? sector.name() : "UNKNOWN")
                            .sectorLabel(sector != null ? sector.getLabel() : "미분류")
                            .investment(e.getValue())
                            .weightPercent(weight)
                            .build();
                })
                .sorted(Comparator.comparing(SectorWeightResponse::getWeightPercent).reversed())
                .toList();
    }

    private int calcAnnualDividend(Long userId, int year) {
        Map<String, Object> annual = dividendService.getAnnual(userId, year);
        return ((Number) annual.get("totalExpectedAmount")).intValue();
    }
}
