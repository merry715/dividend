package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.request.TransactionCreateRequest;
import com.example.dividend.dto.request.TransactionUpdateRequest;
import com.example.dividend.entity.Transaction;
import com.example.dividend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // 전체 거래 내역 조회 (연도·유형 필터)
    @GetMapping
    public ApiResponse<List<Transaction>> getAll(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(transactionService.getAll(year, type));
    }

    // 거래 입력
    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> add(
            @RequestBody @Valid TransactionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(transactionService.add(request), "거래가 등록되었습니다"));
    }

    // 거래 수정
    @PatchMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Transaction>> update(
            @PathVariable Long transactionId,
            @RequestBody @Valid TransactionUpdateRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(transactionService.update(transactionId, request)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "TRANSACTION_NOT_FOUND"));
        }
    }

    // 거래 삭제
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long transactionId) {
        try {
            transactionService.delete(transactionId);
            return ResponseEntity.ok(ApiResponse.ok(null, "거래가 삭제되었습니다"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "TRANSACTION_NOT_FOUND"));
        }
    }

    // 종목별 거래 조회
    @GetMapping("/stocks/{stockId}")
    public ApiResponse<List<Transaction>> getByStock(@PathVariable Long stockId) {
        return ApiResponse.ok(transactionService.getByStockId(stockId));
    }

    // 거래 요약
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary() {
        return ApiResponse.ok(transactionService.getSummary());
    }

    // 전체 종목 보유 현황 (수량 + 평균 단가)
    @GetMapping("/holdings")
    public ApiResponse<List<Map<String, Object>>> getAllHoldings() {
        return ApiResponse.ok(transactionService.getAllHoldings());
    }

    // 특정 종목 보유 현황
    @GetMapping("/stocks/{stockId}/holdings")
    public ApiResponse<Map<String, Object>> getStockHolding(@PathVariable Long stockId) {
        return ApiResponse.ok(transactionService.getStockHolding(stockId));
    }

    // 월별 매수/매도 추이
    @GetMapping("/chart")
    public ApiResponse<Map<String, Object>> getChart(@RequestParam int year) {
        return ApiResponse.ok(transactionService.getMonthlyChart(year));
    }
}
