package com.example.dividend.service;

import com.example.dividend.entity.Transaction;
import com.example.dividend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 보유수량·평균단가 계산 공통 서비스.
 *
 * <p>보유수량 = Σ BUY quantity - Σ SELL quantity (Transaction 집계)
 * <p>평균단가 = Σ(BUY qty × price) / Σ BUY qty
 *
 * <p>모든 보유수량 참조는 Stock.quantity 직접 사용 대신 이 서비스를 통해야 한다.
 * Stock.quantity / Stock.avgPrice 컬럼은 레거시로 유지하되 계산에는 사용하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldingService {

    private final TransactionRepository transactionRepository;

    // ── 공개 DTO ─────────────────────────────────────────────────────────────

    /**
     * 단일 종목 보유 정보 (수량 + 평균단가).
     * 거래내역이 없으면 ZERO 반환.
     */
    public record HoldingInfo(int netQuantity, BigDecimal avgPrice) {
        public static final HoldingInfo ZERO = new HoldingInfo(0, BigDecimal.ZERO);
    }

    // ── 단일 종목 조회 ────────────────────────────────────────────────────────

    /** 단일 종목 보유수량 (Transaction 집계) */
    public int getNetQuantity(Long stockId) {
        return transactionRepository.calculateNetQuantity(stockId);
    }

    /** 단일 종목 보유 정보 — userId 스코프 (userId + stockId 기반) */
    public HoldingInfo getHolding(Long stockId, Long userId) {
        List<Transaction> txs = transactionRepository.findByUserIdAndStockId(userId, stockId);
        return compute(txs);
    }

    // ── 사용자 전체 종목 일괄 조회 (N+1 방지) ─────────────────────────────────

    /**
     * 사용자의 모든 종목 보유 정보 Map.
     * stockId → HoldingInfo. 거래내역 없는 종목은 포함되지 않음(필요 시 getOrDefault(ZERO)).
     */
    public Map<Long, HoldingInfo> getHoldingMap(Long userId) {
        List<Transaction> txs = transactionRepository.findByUserId(userId);
        return buildMap(txs);
    }

    // ── 내부 계산 유틸 ────────────────────────────────────────────────────────

    private HoldingInfo compute(List<Transaction> txs) {
        if (txs == null || txs.isEmpty()) return HoldingInfo.ZERO;

        int netQty = 0, buyQty = 0;
        long buyCost = 0;

        for (Transaction t : txs) {
            if ("BUY".equals(t.getType())) {
                netQty  += t.getQuantity();
                buyQty  += t.getQuantity();
                buyCost += (long) t.getQuantity() * t.getPrice();
            } else {
                netQty -= t.getQuantity();
            }
        }

        BigDecimal avgPx = buyQty > 0
                ? BigDecimal.valueOf(buyCost)
                        .divide(BigDecimal.valueOf(buyQty), 0, RoundingMode.DOWN)
                : BigDecimal.ZERO;

        return new HoldingInfo(netQty, avgPx);
    }

    private Map<Long, HoldingInfo> buildMap(List<Transaction> txs) {
        Map<Long, Integer> netQtyMap  = new HashMap<>();
        Map<Long, Integer> buyQtyMap  = new HashMap<>();
        Map<Long, Long>    buyCostMap = new HashMap<>();

        for (Transaction t : txs) {
            Long sId = t.getStockId();
            if ("BUY".equals(t.getType())) {
                netQtyMap .merge(sId,  t.getQuantity(),               Integer::sum);
                buyQtyMap .merge(sId,  t.getQuantity(),               Integer::sum);
                buyCostMap.merge(sId, (long) t.getQuantity() * t.getPrice(), Long::sum);
            } else {
                netQtyMap.merge(sId, -t.getQuantity(), Integer::sum);
            }
        }

        Map<Long, HoldingInfo> result = new HashMap<>();
        for (Long sId : netQtyMap.keySet()) {
            int  nQty  = netQtyMap .getOrDefault(sId, 0);
            int  bQty  = buyQtyMap .getOrDefault(sId, 0);
            long bCost = buyCostMap.getOrDefault(sId, 0L);
            BigDecimal avg = bQty > 0
                    ? BigDecimal.valueOf(bCost)
                            .divide(BigDecimal.valueOf(bQty), 0, RoundingMode.DOWN)
                    : BigDecimal.ZERO;
            result.put(sId, new HoldingInfo(nQty, avg));
        }
        return result;
    }
}
