package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.request.SectorUpdateRequest;
import com.example.dividend.dto.request.StockCreateRequest;
import com.example.dividend.dto.request.StockUpdateRequest;
import com.example.dividend.dto.response.StockResponse;
import com.example.dividend.dto.response.StockSearchResult;
import com.example.dividend.exception.AccessForbiddenException;
import com.example.dividend.exception.StockNotFoundException;
import com.example.dividend.service.StockSearchService;
import com.example.dividend.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockSearchService stockSearchService;

    // 종목 검색 (자동완성) — /{stockId} 보다 먼저 선언
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StockSearchResult>>> search(
            @AuthenticationPrincipal Long userId,
            @RequestParam String keyword) {
        List<StockSearchResult> results = stockSearchService.search(keyword);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    // 내 종목 전체 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<StockResponse>>> getAll(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getAll(userId)));
    }

    // 종목 단건 조회
    @GetMapping("/{stockId}")
    public ResponseEntity<ApiResponse<StockResponse>> getById(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stockId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockService.getById(stockId, userId)));
        } catch (StockNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND"));
        } catch (AccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }

    // 종목 추가
    @PostMapping
    public ResponseEntity<ApiResponse<StockResponse>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid StockCreateRequest request) {
        try {
            StockResponse response = stockService.create(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "종목이 추가되었습니다"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "DUPLICATE_STOCK_CODE"));
        }
    }

    // 종목 수정 (수정할 필드만 포함)
    @PatchMapping("/{stockId}")
    public ResponseEntity<ApiResponse<StockResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stockId,
            @RequestBody @Valid StockUpdateRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockService.update(stockId, userId, request)));
        } catch (StockNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND"));
        } catch (AccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }

    // 섹터 단독 수정
    @PatchMapping("/{stockId}/sector")
    public ResponseEntity<ApiResponse<StockResponse>> updateSector(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stockId,
            @RequestBody @Valid SectorUpdateRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(stockService.updateSector(stockId, userId, request)));
        } catch (StockNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND"));
        } catch (AccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }

    // 종목 삭제 (soft delete)
    @DeleteMapping("/{stockId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stockId) {
        try {
            stockService.delete(stockId, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "종목이 삭제되었습니다"));
        } catch (StockNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND"));
        } catch (AccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }

    // @Valid 유효성 검사 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "INVALID_INPUT"));
    }

    // 잘못된 Enum 값 등 JSON 역직렬화 실패 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("요청 형식이 올바르지 않습니다. 섹터 값을 확인해 주세요 (예: IT, ENERGY)", "INVALID_INPUT"));
    }
}
