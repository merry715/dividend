package com.example.dividend.service;

import com.example.dividend.dto.response.*;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.StockSector;
import com.example.dividend.repository.DividendRepository;
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

    private final StockRepository stockRepository;
    private final DividendRepository dividendRepository;
    private final GoalRepository goalRepository;

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

        BigDecimal totalInvestment = stocks.stream()
                .map(s -> s.getAvgPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int currentYear = LocalDate.now().getYear();
        int totalExpectedDividend = calcAnnualDividend(userId, currentYear);

        return AnalysisSummaryResponse.builder()
                .totalInvestment(totalInvestment)
                .totalExpectedDividend(totalExpectedDividend)
                .stockCount(stocks.size())
                .build();
    }

    public List<AnnualDividendResponse> getAnnualDividends(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_Id(userId);
        if (stocks.isEmpty()) {
            return List.of();
        }

        List<Dividend> dividends = dividendRepository.findByUserId(userId);

        Map<Integer, Integer> byYear = new TreeMap<>(Comparator.reverseOrder());
        for (Dividend d : dividends) {
            int amount = "CONFIRMED".equals(d.getStatus())
                    ? d.getConfirmedDividend()
                    : d.getExpectedDividend();
            byYear.merge(d.getYear(), amount, Integer::sum);
        }

        return byYear.entrySet().stream()
                .map(e -> AnnualDividendResponse.builder()
                        .year(e.getKey())
                        .totalDividend(e.getValue())
                        .build())
                .toList();
    }

    public List<StockWeightResponse> getStockWeights(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_Id(userId);

        BigDecimal totalInvestment = stocks.stream()
                .map(s -> s.getAvgPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return stocks.stream()
                .map(s -> {
                    BigDecimal investment = s.getAvgPrice().multiply(BigDecimal.valueOf(s.getQuantity()));
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

        BigDecimal totalInvestment = stocks.stream()
                .map(s -> s.getAvgPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<StockSector, BigDecimal> bySector = new LinkedHashMap<>();
        for (Stock s : stocks) {
            BigDecimal investment = s.getAvgPrice().multiply(BigDecimal.valueOf(s.getQuantity()));
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
        List<Stock> stocks = stockRepository.findByUser_Id(userId);
        if (stocks.isEmpty()) return 0;

        List<Long> stockIds = stocks.stream().map(Stock::getId).toList();
        return dividendRepository.findByStockIdIn(stockIds).stream()
                .filter(d -> d.getYear() == year)
                .mapToInt(d -> "CONFIRMED".equals(d.getStatus())
                        ? d.getConfirmedDividend()
                        : d.getExpectedDividend())
                .sum();
    }
}
