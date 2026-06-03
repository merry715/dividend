package com.example.dividend.service;

import com.example.dividend.client.DataGoKrDividendClient;
import com.example.dividend.client.DataGoKrDividendInfo;
import com.example.dividend.client.DartNaverClient;
import com.example.dividend.dto.request.SectorUpdateRequest;
import com.example.dividend.dto.request.StockCreateRequest;
import com.example.dividend.dto.request.StockUpdateRequest;
import com.example.dividend.dto.response.StockResponse;
import com.example.dividend.entity.Dividend;
import com.example.dividend.entity.Stock;
import com.example.dividend.entity.User;
import com.example.dividend.exception.AccessForbiddenException;
import com.example.dividend.exception.StockNotFoundException;
import com.example.dividend.repository.DividendRepository;
import com.example.dividend.repository.StockRepository;
import com.example.dividend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository            stockRepository;
    private final UserRepository             userRepository;
    private final DividendRepository         dividendRepository;
    private final DartNaverClient            dartClient;
    private final StockPriceFallbackService  priceService;
    private final DataGoKrDividendClient     dataGoKrClient;
    private final HoldingService             holdingService;
    @Lazy
    private final DividendService            dividendService;

    // ── 종목 전체 조회 (배당·투자금액·투자비중 포함) ─────────────────────────

    public List<StockResponse> getAll(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_IdWithUser(userId);

        // 배당 일괄 조회 (N+1 방지)
        List<Long> stockIds = stocks.stream().map(Stock::getId).toList();
        Map<Long, List<Dividend>> dividendMap = dividendRepository.findByStockIdIn(stockIds)
                .stream()
                .collect(Collectors.groupingBy(Dividend::getStockId));

        // 보유 정보 일괄 조회 (Transaction 집계 — N+1 방지)
        Map<Long, HoldingService.HoldingInfo> holdingMap = holdingService.getHoldingMap(userId);

        // 총 투자금액 (Transaction 기반 netQty × avgPrice)
        BigDecimal totalInvestment = stocks.stream()
                .map(s -> {
                    HoldingService.HoldingInfo h = holdingMap.getOrDefault(
                            s.getId(), HoldingService.HoldingInfo.ZERO);
                    return h.avgPrice().multiply(
                            BigDecimal.valueOf(Math.max(h.netQuantity(), 0)));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return stocks.stream()
                .map(s -> {
                    HoldingService.HoldingInfo h = holdingMap.getOrDefault(
                            s.getId(), HoldingService.HoldingInfo.ZERO);
                    return StockResponse.of(
                            s,
                            dividendMap.getOrDefault(s.getId(), List.of()),
                            totalInvestment,
                            h.netQuantity(),
                            h.avgPrice());
                })
                .toList();
    }

    // ── 종목 단건 조회 (소유권 체크 포함) ────────────────────────────────────

    public StockResponse getById(Long stockId, Long userId) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);
        HoldingService.HoldingInfo h = holdingService.getHolding(stockId, userId);
        return StockResponse.from(stock, h.netQuantity(), h.avgPrice());
    }

    // ── 종목 등록 ─────────────────────────────────────────────────────────────

    @Transactional
    public StockResponse create(StockCreateRequest req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + userId));

        String stockCode = req.getStockCode().trim().toUpperCase();

        // 중복 종목 여부 확인
        if (stockRepository.existsByUser_IdAndStockCode(userId, stockCode)) {
            throw new IllegalArgumentException("이미 등록된 종목 코드입니다: " + stockCode);
        }

        // 소프트딜리트된 동일 종목이 있으면 복원해서 재사용 (DB unique 제약 방어)
        Stock stock = stockRepository.findDeletedByUserIdAndStockCode(userId, stockCode)
                .orElse(new Stock());

        stock.setUser(user);
        stock.setStockName(req.getStockName().trim());
        stock.setStockCode(stockCode);
        stock.setSector(req.getSector());
        stock.setExchange(req.getExchange() != null ? req.getExchange().toUpperCase() : "KOSPI");
        stock.setCurrency("KRW");
        // 보유수량/단가는 Transaction 집계로 계산하므로 Stock에 저장하지 않음
        stock.setQuantity(0);
        stock.setAvgPrice(BigDecimal.ZERO);
        stock.setDividendCycle(req.getDividendCycle());
        stock.setExpectedDividendPerShare(null);
        stock.setPreviousClose(null);
        stock.restore(); // deletedAt = null (새 Stock이면 no-op)

        // 주당 배당금이 입력됐으면 그대로 사용, 없으면 DART에서 자동 조회
        if (req.getExpectedDividendPerShare() != null) {
            stock.setExpectedDividendPerShare(req.getExpectedDividendPerShare());
        } else {
            fetchAndSetDartDividend(stock, stockCode);
        }

        Stock saved = stockRepository.save(stock);

        // 등록 직후 현재가 조회 → 저장 (네이버·캐시 기반), avg_purchase fallback도 허용
        try {
            com.example.dividend.dto.PriceResult pr = priceService.fetchPriceForStock(saved);
            boolean isRealPrice = com.example.dividend.dto.PriceResult.SOURCE_YFINANCE.equals(pr.getSource())
                    || com.example.dividend.dto.PriceResult.SOURCE_CACHE.equals(pr.getSource());
            if (isRealPrice && pr.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                saved.setPreviousClose(pr.getPrice());
                saved.setPriceSource(pr.getSource());
                stockRepository.save(saved);
            }
        } catch (Exception e) {
            log.warn("종목 등록 후 가격 조회 실패 [stockCode={}]: {}", stockCode, e.getMessage());
        }

        // 종목 등록 후 배당 row 생성
        try {
            int year = java.time.LocalDate.now().getYear();
            // DART 공식 회사명 우선 사용 (공공데이터 API 검색명과 일치)
            // 예: 사용자 입력 "신한지주" → DART 공식명 "신한금융지주회사"
            String apiCompanyName = dartClient.findCorpName(stockCode);
            if (apiCompanyName == null) apiCompanyName = saved.getStockName();

            List<DataGoKrDividendInfo> apiRecords =
                    dataGoKrClient.fetchAllDividendRecords(stockCode, apiCompanyName, year - 1);
            List<DataGoKrDividendInfo> validRecords = apiRecords.stream()
                    .filter(r -> r.getPayDate() != null)
                    .sorted(java.util.Comparator.comparing(r -> r.getPayDate().getMonthValue()))
                    .collect(java.util.stream.Collectors.toList());

            // 배당 주기 판별: dvdnBasDt 월 간격 → count → DART → 기존값 순서
            String cycle = dividendService.detectCycleFromBaseDtMonths(validRecords);
            if (cycle == null) cycle = dividendService.detectCycle(validRecords.size());
            if (cycle == null) cycle = dartClient.detectDividendCycle(stockCode, year - 1);
            if (cycle == null) cycle = saved.getDividendCycle();

            // DART에서 감지한 cycle을 Stock 엔티티에 저장
            // 저장하지 않으면 generateExpectedDividends fallback이 ANNUAL로 오판함
            if (cycle != null && saved.getDividendCycle() == null) {
                saved.setDividendCycle(cycle);
                stockRepository.save(saved);
                log.info("배당 주기 저장 [stockCode={}, cycle={}]", stockCode, cycle);
            }

            // 공공데이터 API records가 있으면 실데이터 기반 row 생성
            if (!validRecords.isEmpty()) {
                dividendService.generateFromApiData(userId, saved.getId(), year, validRecords, cycle);
                log.info("공공데이터 기반 배당 row 생성 완료 [stockCode={}, records={}건]",
                        stockCode, validRecords.size());
            } else {
                // 공공데이터 없음 → cycle + expectedDividendPerShare 기반으로 즉시 fallback 생성
                // (프론트 호출을 기다리지 않고 등록 시점에 바로 row 생성)
                if (cycle != null && saved.getExpectedDividendPerShare() != null
                        && saved.getExpectedDividendPerShare() > 0) {
                    List<com.example.dividend.entity.Dividend> generated =
                            dividendService.generateExpectedDividends(userId, saved.getId(), year);
                    log.info("배당 fallback 생성 완료 [stockCode={}, cycle={}, 생성건수={}]",
                            stockCode, cycle, generated.size());
                } else {
                    log.info("배당 정보 부족 — row 생성 건너뜀 [stockCode={}, cycle={}, perShare={}]",
                            stockCode, cycle, saved.getExpectedDividendPerShare());
                }
            }
        } catch (Exception e) {
            log.warn("종목 등록 후 배당 생성 실패 [stockCode={}]: {}", stockCode, e.getMessage());
        }

        return StockResponse.from(saved);
    }

    // ── 종목 수정 ─────────────────────────────────────────────────────────────

    @Transactional
    public StockResponse update(Long stockId, Long userId, StockUpdateRequest req) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);

        if (req.getStockName()                != null) stock.setStockName(req.getStockName().trim());
        if (req.getSector()                   != null) stock.setSector(req.getSector());
        if (req.getExchange()                 != null) stock.setExchange(req.getExchange().toUpperCase());
        // quantity / avgPrice는 Transaction 집계로 관리 → Stock 저장 무시
        if (req.getDividendCycle()            != null) stock.setDividendCycle(req.getDividendCycle());
        if (req.getExpectedDividendPerShare() != null) stock.setExpectedDividendPerShare(req.getExpectedDividendPerShare());

        Stock saved = stockRepository.save(stock);
        HoldingService.HoldingInfo h = holdingService.getHolding(stockId, userId);
        return StockResponse.from(saved, h.netQuantity(), h.avgPrice());
    }

    // ── 섹터 단독 수정 ────────────────────────────────────────────────────────

    @Transactional
    public StockResponse updateSector(Long stockId, Long userId, SectorUpdateRequest req) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);
        stock.setSector(req.getSector());
        Stock saved = stockRepository.save(stock);
        HoldingService.HoldingInfo h = holdingService.getHolding(stockId, userId);
        return StockResponse.from(saved, h.netQuantity(), h.avgPrice());
    }

    // ── 종목 삭제 (soft delete) ───────────────────────────────────────────────

    @Transactional
    public void delete(Long stockId, Long userId) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);
        stock.softDelete();
        stockRepository.save(stock);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    /**
     * DART에서 최근 사업연도 주당 배당금을 조회해 종목에 설정.
     * 실패해도 예외를 던지지 않고 무시합니다 (종목 등록은 항상 성공).
     */
    private void fetchAndSetDartDividend(Stock stock, String stockCode) {
        try {
            String corpCode = dartClient.findCorpCode(stockCode);
            if (corpCode == null) {
                log.info("DART 기업코드 없음 [stockCode={}]", stockCode);
                return;
            }

            int baseYear = java.time.LocalDate.now().getYear() - 1;
            DartNaverClient.DartDividendInfo info = null;
            int year = baseYear;
            // 당해년도 데이터 없으면 전년도 재시도
            for (int y = baseYear; y >= baseYear - 1; y--) {
                info = dartClient.fetchDividendInfo(corpCode, y);
                if (info != null && info.getDividendPerShare() > 0) { year = y; break; }
            }
            if (info != null && info.getDividendPerShare() > 0) {
                stock.setExpectedDividendPerShare((int) info.getDividendPerShare());
                log.info("DART 배당금 자동 설정 [stockCode={}, 주당배당금={}, 연도={}]",
                        stockCode, info.getDividendPerShare(), year);
            }
        } catch (Exception e) {
            log.warn("DART 배당금 자동 조회 실패 [stockCode={}]: {}", stockCode, e.getMessage());
        }
    }

    private Stock findStock(Long stockId) {
        return stockRepository.findByIdWithUser(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));
    }

    private void validateOwnership(Stock stock, Long userId) {
        if (!stock.getUser().getId().equals(userId)) {
            throw new AccessForbiddenException("해당 종목에 대한 접근 권한이 없습니다");
        }
    }
}
