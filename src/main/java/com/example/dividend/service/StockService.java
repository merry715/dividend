package com.example.dividend.service;

import com.example.dividend.client.PythonServerClient;
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

    private final StockRepository        stockRepository;
    private final UserRepository         userRepository;
    private final DividendRepository     dividendRepository;
    private final PythonServerClient     dartClient;
    private final StockPriceFallbackService priceService;

    // 내 종목 전체 조회 (평가손익·투자비중·배당 포함)
    public List<StockResponse> getAll(Long userId) {
        List<Stock> stocks = stockRepository.findByUser_IdWithUser(userId);

        // 배당 일괄 조회 (N+1 방지)
        List<Long> stockIds = stocks.stream().map(Stock::getId).toList();
        Map<Long, List<Dividend>> dividendMap = dividendRepository.findByStockIdIn(stockIds)
                .stream()
                .collect(Collectors.groupingBy(Dividend::getStockId));

        // 총 투자원금 (investmentWeight 분모)
        BigDecimal totalInvestment = stocks.stream()
                .map(s -> s.getAvgPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return stocks.stream()
                .map(s -> StockResponse.of(
                        s,
                        dividendMap.getOrDefault(s.getId(), List.of()),
                        totalInvestment))
                .toList();
    }

    // 종목 단건 조회 (소유권 검증 포함)
    public StockResponse getById(Long stockId, Long userId) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);
        return StockResponse.from(stock);
    }

    // 종목 추가
    @Transactional
    public StockResponse create(StockCreateRequest req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + userId));

        String stockCode = req.getStockCode().trim().toUpperCase();

        // 활성 종목 중복 확인
        if (stockRepository.existsByUser_IdAndStockCode(userId, stockCode)) {
            throw new IllegalArgumentException("이미 등록된 종목 코드입니다: " + stockCode);
        }

        // 소프트 딜리트된 동일 종목이 있으면 복원해서 재사용 (DB unique 제약 회피)
        Stock stock = stockRepository.findDeletedByUserIdAndStockCode(userId, stockCode)
                .orElse(new Stock());

        stock.setUser(user);
        stock.setStockName(req.getStockName().trim());
        stock.setStockCode(stockCode);
        stock.setSector(req.getSector());
        stock.setExchange(req.getExchange() != null ? req.getExchange().toUpperCase() : "KOSPI");
        stock.setCurrency("KRW");
        stock.setQuantity(req.getQuantity() != null ? req.getQuantity() : 0);
        stock.setAvgPrice(req.getAvgPrice() != null ? req.getAvgPrice() : BigDecimal.ZERO);
        stock.setDividendCycle(req.getDividendCycle());
        stock.setExpectedDividendPerShare(null);
        stock.setPreviousClose(null);
        stock.restore(); // deletedAt = null (이미 새 Stock이면 no-op)

        // 주당 배당금: 요청에 값이 있으면 우선 사용, 없으면 DART에서 자동 조회
        if (req.getExpectedDividendPerShare() != null) {
            stock.setExpectedDividendPerShare(req.getExpectedDividendPerShare());
        } else {
            fetchAndSetDartDividend(stock, stockCode);
        }

        Stock saved = stockRepository.save(stock);

        // 저장 직후 즉시 현재가 조회 — 실제 시세(네이버/캐시)만 반영, avg_purchase fallback은 무시
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
            log.warn("종목 추가 후 가격 조회 실패 [stockCode={}]: {}", stockCode, e.getMessage());
        }

        return StockResponse.from(saved);
    }

    // 종목 수정
    @Transactional
    public StockResponse update(Long stockId, Long userId, StockUpdateRequest req) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);

        if (req.getStockName()               != null) stock.setStockName(req.getStockName().trim());
        if (req.getSector()                  != null) stock.setSector(req.getSector());
        if (req.getExchange()                != null) stock.setExchange(req.getExchange().toUpperCase());
        if (req.getQuantity()                != null) stock.setQuantity(req.getQuantity());
        if (req.getAvgPrice()                != null) stock.setAvgPrice(req.getAvgPrice());
        if (req.getDividendCycle()           != null) stock.setDividendCycle(req.getDividendCycle());
        if (req.getExpectedDividendPerShare() != null) stock.setExpectedDividendPerShare(req.getExpectedDividendPerShare());

        return StockResponse.from(stockRepository.save(stock));
    }

    // 섹터 단독 수정
    @Transactional
    public StockResponse updateSector(Long stockId, Long userId, SectorUpdateRequest req) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);
        stock.setSector(req.getSector());
        return StockResponse.from(stockRepository.save(stock));
    }

    // 종목 삭제 (soft delete)
    @Transactional
    public void delete(Long stockId, Long userId) {
        Stock stock = findStock(stockId);
        validateOwnership(stock, userId);
        stock.softDelete();
        stockRepository.save(stock);
    }

    // ── 내부 유틸 ─────────────────────────────────────────────

    /**
     * DART에서 최근 사업연도 주당 배당금을 조회해 종목에 설정.
     * 실패해도 예외를 던지지 않고 조용히 넘어감 (종목 등록은 항상 성공).
     */
    private void fetchAndSetDartDividend(Stock stock, String stockCode) {
        try {
            String corpCode = dartClient.findCorpCode(stockCode);
            if (corpCode == null) {
                log.info("DART 기업코드 없음 [stockCode={}]", stockCode);
                return;
            }

            int year = java.time.LocalDate.now().getYear() - 1; // 전년도 사업보고서
            PythonServerClient.DartDividendInfo info = dartClient.fetchDividendInfo(corpCode, year);
            if (info != null && info.getDividendPerShare() > 0) {
                stock.setExpectedDividendPerShare((int) info.getDividendPerShare());
                log.info("DART 배당금 자동 설정 [stockCode={}, 주당배당금={}원, 연도={}]",
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
            throw new AccessForbiddenException("해당 종목에 접근할 권한이 없습니다");
        }
    }
}
