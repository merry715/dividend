package com.example.dividend.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final DividendRepository dividendRepository;

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

        if (stockRepository.existsByUser_IdAndStockCode(userId, stockCode)) {
            throw new IllegalArgumentException("이미 등록된 종목 코드입니다: " + stockCode);
        }

        Stock stock = new Stock();
        stock.setUser(user);
        stock.setStockName(req.getStockName().trim());
        stock.setStockCode(stockCode);
        stock.setSector(req.getSector());
        stock.setExchange(req.getExchange() != null ? req.getExchange().toUpperCase() : "KOSPI");
        stock.setCurrency("KRW"); // KRX 전용: 통화는 항상 KRW
        stock.setQuantity(req.getQuantity() != null ? req.getQuantity() : 0);
        stock.setAvgPrice(req.getAvgPrice() != null ? req.getAvgPrice() : BigDecimal.ZERO);
        stock.setDividendCycle(req.getDividendCycle());
        stock.setExpectedDividendPerShare(req.getExpectedDividendPerShare());

        return StockResponse.from(stockRepository.save(stock));
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
