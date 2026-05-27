package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.request.DividendConfirmRequest;
import com.example.dividend.dto.request.DividendGenerateRequest;
import com.example.dividend.entity.Dividend;
import com.example.dividend.service.DividendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/dividends")
@RequiredArgsConstructor
public class DividendController {

    private final DividendService dividendService;

    // [1] 예상 배당 자동 생성
    @PostMapping("/generate")
    public ApiResponse<List<Dividend>> generate(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid DividendGenerateRequest request) {
        List<Dividend> created = dividendService.generateExpectedDividends(
                userId, request.getStockId(), request.getYear());
        return ApiResponse.ok(created, "예상 배당이 생성되었습니다");
    }

    // [2] 배당 확정 (EXPECTED → CONFIRMED)
    @PatchMapping("/{dividendId}/confirm")
    public ApiResponse<Dividend> confirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long dividendId,
            @RequestBody @Valid DividendConfirmRequest request) {
        return ApiResponse.ok(dividendService.confirm(dividendId, userId, request), "배당이 확정되었습니다");
    }

    // [3] 월별 배당금 조회
    @GetMapping("/monthly")
    public ApiResponse<List<Map<String, Object>>> getMonthly(
            @AuthenticationPrincipal Long userId,
            @RequestParam Optional<Integer> year) {
        int y = year.orElse(LocalDate.now().getYear());
        return ApiResponse.ok(dividendService.getMonthly(userId, y));
    }

    // [4] 연간 예상 배당금
    @GetMapping("/annual")
    public ApiResponse<Map<String, Object>> getAnnual(
            @AuthenticationPrincipal Long userId,
            @RequestParam Optional<Integer> year) {
        int y = year.orElse(LocalDate.now().getYear());
        return ApiResponse.ok(dividendService.getAnnual(userId, y));
    }

    // [5] 누적 배당금 조회
    @GetMapping("/cumulative")
    public ApiResponse<Map<String, Object>> getCumulative(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(dividendService.getCumulative(userId));
    }

    // [6] 연도별 배당금 조회
    @GetMapping("/yearly")
    public ApiResponse<List<Map<String, Object>>> getYearly(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(dividendService.getYearly(userId));
    }
    @GetMapping
    public ApiResponse<List<Dividend>> getAll(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(dividendService.getAll(userId));
    }
}