package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.entity.Stock;
import com.example.dividend.service.StockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // 전체 보유 종목 조회
    @GetMapping
    public ApiResponse<List<Stock>> getAll() {
        return ApiResponse.ok(stockService.getAll());
    }

    // 종목 추가
    @PostMapping
    public ApiResponse<Stock> add(@RequestBody Stock stock) {
        return ApiResponse.ok(stockService.add(stock), "종목이 추가되었습니다");
    }

    // 종목 수정
    @PatchMapping("/{id}")
    public ApiResponse<Stock> update(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            return ApiResponse.ok(stockService.update(id, req));
        } catch (NoSuchElementException e) {
            return ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND");
        }
    }

    // 종목 삭제
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        try {
            stockService.delete(id);
            return ApiResponse.ok(null, "종목이 삭제되었습니다");
        } catch (NoSuchElementException e) {
            return ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND");
        }
    }

    // 종목명·코드 검색
    @GetMapping("/search")
    public ApiResponse<List<Stock>> search(@RequestParam String keyword) {
        return ApiResponse.ok(stockService.search(keyword));
    }

    // 섹터 정보 등록·수정
    @PatchMapping("/{id}/sector")
    public ApiResponse<Stock> updateSector(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            String sector = req.get("sector").toString();
            return ApiResponse.ok(stockService.updateSector(id, sector));
        } catch (NoSuchElementException e) {
            return ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND");
        }
    }
}
