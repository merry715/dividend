package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.entity.Transaction;
import com.example.dividend.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // 전체 거래 내역 조회 (연도·유형 필터)
    @GetMapping
    public ApiResponse<List<Transaction>> getAll(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(transactionService.getAll(year, type));
    }

    // 거래 입력
    @PostMapping
    public ApiResponse<Transaction> add(@RequestBody Map<String, Object> req) {
        return ApiResponse.ok(transactionService.add(req), "거래가 등록되었습니다");
    }

    // 거래 수정
    @PatchMapping("/{transactionId}")
    public ApiResponse<Transaction> update(
            @PathVariable Long transactionId,
            @RequestBody Map<String, Object> req) {
        try {
            return ApiResponse.ok(transactionService.update(transactionId, req));
        } catch (NoSuchElementException e) {
            return ApiResponse.error(e.getMessage(), "TRANSACTION_NOT_FOUND");
        }
    }

    // 거래 삭제
    @DeleteMapping("/{transactionId}")
    public ApiResponse<Void> delete(@PathVariable Long transactionId) {
        try {
            transactionService.delete(transactionId);
            return ApiResponse.ok(null, "거래가 삭제되었습니다");
        } catch (NoSuchElementException e) {
            return ApiResponse.error(e.getMessage(), "TRANSACTION_NOT_FOUND");
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

    // 월별 매수/매도 추이
    @GetMapping("/chart")
    public ApiResponse<Map<String, Object>> getChart(@RequestParam int year) {
        return ApiResponse.ok(transactionService.getMonthlyChart(year));
    }
}
