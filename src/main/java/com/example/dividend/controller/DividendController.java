package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.request.DividendConfirmRequest;
import com.example.dividend.dto.request.DividendGenerateRequest;
import com.example.dividend.entity.Dividend;
import com.example.dividend.service.DividendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/dividends")
@RequiredArgsConstructor
public class DividendController {

    private final DividendService dividendService;

    // 전체 배당 조회
    @GetMapping
    public ApiResponse<List<Dividend>> getAll() {
        return ApiResponse.ok(dividendService.getAll());
    }

    // 예상 배당 자동 생성
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<List<Dividend>>> generate(
            @RequestBody @Valid DividendGenerateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(dividendService.generate(request), "예상 배당이 생성되었습니다"));
    }

    // 배당 확정 전환
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Dividend>> confirm(
            @PathVariable Long id,
            @RequestBody @Valid DividendConfirmRequest request) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.ok(dividendService.confirm(id, request), "배당이 확정되었습니다"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "DIVIDEND_NOT_FOUND"));
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
