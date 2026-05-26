package com.example.dividend.service;

import com.example.dividend.dto.response.EvaluationResponse;
import com.example.dividend.dto.response.EvaluationSummaryResponse;
import com.example.dividend.entity.Stock;
import com.example.dividend.exception.AccessForbiddenException;
import com.example.dividend.exception.StockNotFoundException;
import com.example.dividend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final StockRepository stockRepository;

    public EvaluationResponse getEvaluation(Long stockId, Long userId) {
        Stock stock = stockRepository.findByIdWithUser(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));

        if (!stock.getUser().getId().equals(userId)) {
            throw new AccessForbiddenException("해당 종목에 접근할 권한이 없습니다");
        }
        return buildEvaluation(stock);
    }

    public EvaluationSummaryResponse getSummary(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_Id(userId);

        BigDecimal totalInvestment  = BigDecimal.ZERO;
        BigDecimal totalEvaluation  = BigDecimal.ZERO;
        int priceAvailable = 0;

        for (Stock stock : stocks) {
            if (!hasPreviousClose(stock)) continue;

            BigDecimal qty        = BigDecimal.valueOf(stock.getQuantity());
            BigDecimal investment = stock.getAvgPrice().multiply(qty);
            BigDecimal evaluation = stock.getPreviousClose().multiply(qty);

            totalInvestment  = totalInvestment.add(investment);
            totalEvaluation  = totalEvaluation.add(evaluation);
            priceAvailable++;
        }

        BigDecimal totalGain       = totalEvaluation.subtract(totalInvestment);
        BigDecimal totalReturnRate = calcReturnRate(totalGain, totalInvestment);

        return EvaluationSummaryResponse.builder()
                .totalStocks(stocks.size())
                .priceAvailableStocks(priceAvailable)
                .totalInvestment(totalInvestment)
                .totalEvaluation(totalEvaluation)
                .totalGain(totalGain)
                .totalReturnRate(totalReturnRate)
                .build();
    }

    // ── 내부 유틸 ───────────────────────────────────────────────────────────────

    private EvaluationResponse buildEvaluation(Stock stock) {
        boolean hasPrice = hasPreviousClose(stock);

        BigDecimal evalAmount  = null;
        BigDecimal evalGain    = null;
        BigDecimal returnRate  = null;

        if (hasPrice) {
            BigDecimal qty        = BigDecimal.valueOf(stock.getQuantity());
            BigDecimal investment = stock.getAvgPrice().multiply(qty);
            evalAmount = stock.getPreviousClose().multiply(qty);
            evalGain   = evalAmount.subtract(investment);
            returnRate = calcReturnRate(evalGain, investment);
        }

        return EvaluationResponse.builder()
                .stockId(stock.getId())
                .stockCode(stock.getStockCode())
                .stockName(stock.getStockName())
                .currency(stock.getCurrency())
                .quantity(stock.getQuantity())
                .avgPrice(stock.getAvgPrice())
                .previousClose(hasPrice ? stock.getPreviousClose() : null)
                .evaluationAmount(evalAmount)
                .evaluationGain(evalGain)
                .returnRate(returnRate)
                .build();
    }

    private boolean hasPreviousClose(Stock stock) {
        BigDecimal close = stock.getPreviousClose();
        return close != null && close.compareTo(BigDecimal.ZERO) > 0;
    }

    /** 수익률(%) = (gain / investment) × 100, 소수점 2자리. 원금 0이면 null. */
    private BigDecimal calcReturnRate(BigDecimal gain, BigDecimal investment) {
        if (investment == null || investment.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return gain.divide(investment, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
