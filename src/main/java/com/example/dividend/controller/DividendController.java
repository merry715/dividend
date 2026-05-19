package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.entity.Dividend;
import com.example.dividend.service.DividendService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/dividends")
public class DividendController {

    private final DividendService dividendService;

    public DividendController(DividendService dividendService) {
        this.dividendService = dividendService;
    }

    // 전체 배당 조회
    @GetMapping
    public ApiResponse<List<Dividend>> getAll() {
        return ApiResponse.ok(dividendService.getAll());
    }

    // 예상 배당 자동 생성
    @PostMapping("/generate")
    public ApiResponse<List<Dividend>> generate(@RequestBody Map<String, Object> req) {
        return ApiResponse.ok(dividendService.generate(req), "예상 배당이 생성되었습니다");
    }

    // 확정 전환
    @PatchMapping("/{id}")
    public ApiResponse<Dividend> confirm(
            @PathVariable Long id,
            @RequestBody Map<String, Object> req) {
        try {
            return ApiResponse.ok(dividendService.confirm(id, req), "배당이 확정되었습니다");
        } catch (NoSuchElementException e) {
            return ApiResponse.error(e.getMessage(), "DIVIDEND_NOT_FOUND");
        }
    }

    // 월별 예상·확정 배당 조회
    @GetMapping("/monthly")
    public ApiResponse<List<Map<String, Object>>> getMonthly() {
        return ApiResponse.ok(dividendService.getMonthly());
    }

    // 연간 배당 합계
    @GetMapping("/annual")
    public ApiResponse<Map<String, Object>> getAnnual() {
        return ApiResponse.ok(dividendService.getAnnual());
    }

    // 누적 배당 조회
    @GetMapping("/cumulative")
    public ApiResponse<List<Map<String, Object>>> getCumulative() {
        return ApiResponse.ok(dividendService.getCumulative());
    }

    // 연도별 배당 조회
    @GetMapping("/yearly")
    public ApiResponse<List<Map<String, Object>>> getYearly() {
        return ApiResponse.ok(dividendService.getYearly());
    }

    // 종목별 배당 정보
    @GetMapping("/by-stock")
    public ApiResponse<List<Map<String, Object>>> getByStock() {
        return ApiResponse.ok(dividendService.getByStock());
    }
}
