package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.request.DividendAutoGenerateRequest;
import com.example.dividend.dto.request.DividendConfirmRequest;
import com.example.dividend.dto.request.DividendGenerateRequest;
import com.example.dividend.dto.request.DividendUpdateRequest;
import com.example.dividend.dto.response.DividendAutoGenerateResponse;
import com.example.dividend.dto.response.StockDividendResponse;
import com.example.dividend.entity.Dividend;
import com.example.dividend.service.DividendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // [NEW-1] 분기/반기 배당 첫 번째 지급일 확정 + 나머지 자동생성
    @PostMapping("/confirm-with-auto-generate")
    public ApiResponse<DividendAutoGenerateResponse> confirmWithAutoGenerate(
            @AuthenticationPrincipal Long userId,
            @RequestBody DividendAutoGenerateRequest request) {
        return ApiResponse.ok(
                dividendService.confirmWithAutoGenerate(userId, request),
                "배당이 확정 및 자동 생성되었습니다"
        );
    }

    // [NEW-2] 개별 배당 row 업데이트
    @PatchMapping("/{dividendId}")
    public ApiResponse<Dividend> updateDividend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long dividendId,
            @RequestBody DividendUpdateRequest request) {
        return ApiResponse.ok(
                dividendService.updateDividend(dividendId, userId, request),
                "배당이 업데이트되었습니다"
        );
    }

    // [NEW-3] 종목별 배당 정보 조회
    @GetMapping("/by-stock")
    public ApiResponse<List<StockDividendResponse>> getByStock(
            @AuthenticationPrincipal Long userId,
            @RequestParam Optional<Integer> year) {
        int y = year.orElse(LocalDate.now().getYear());
        return ApiResponse.ok(dividendService.getByStock(userId, y));
    }

    // [기존] 확정 전환 드롭다운용 종목 목록
    @GetMapping("/stocks-for-confirm")
    public ApiResponse<List<Map<String, Object>>> getStocksForConfirm(
            @AuthenticationPrincipal Long userId,
            @RequestParam Optional<Integer> year) {
        int y = year.orElse(LocalDate.now().getYear());
        return ApiResponse.ok(dividendService.getStocksForConfirm(userId, y));
    }
}