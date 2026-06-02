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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ApiResponse<List<Transaction>> getAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type) {
        return ApiResponse.ok(transactionService.getAll(userId, year, type));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Transaction>> add(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid TransactionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(transactionService.add(userId, request), "거래가 등록되었습니다"));
    }

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

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long transactionId) {
        try {
            transactionService.delete(transactionId);
            return ResponseEntity.ok(ApiResponse.ok(null, "거래가 삭제되었습니다"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "TRANSACTION_NOT_FOUND"));
        }
    }

    @GetMapping("/stocks/{stockId}")
    public ApiResponse<List<Transaction>> getByStock(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stockId) {
        return ApiResponse.ok(transactionService.getByStockId(userId, stockId));
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(transactionService.getSummary(userId));
    }

    @GetMapping("/holdings")
    public ApiResponse<List<Map<String, Object>>> getAllHoldings(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(transactionService.getAllHoldings(userId));
    }

    @GetMapping("/stocks/{stockId}/holdings")
    public ApiResponse<Map<String, Object>> getStockHolding(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stockId) {
        return ApiResponse.ok(transactionService.getStockHolding(userId, stockId));
    }

    @GetMapping("/chart")
    public ApiResponse<Map<String, Object>> getChart(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year) {
        return ApiResponse.ok(transactionService.getMonthlyChart(userId, year));
    }
}
