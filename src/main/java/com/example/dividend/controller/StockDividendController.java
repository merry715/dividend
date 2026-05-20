package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.response.ExpectedDividendResponse;
import com.example.dividend.exception.AccessForbiddenException;
import com.example.dividend.exception.StockNotFoundException;
import com.example.dividend.service.DividendQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks/{stockId}/dividends")
@RequiredArgsConstructor
public class StockDividendController {

    private final DividendQueryService dividendQueryService;

    // 종목별 예상/확정 배당금 조회 + 연간 합산
    @GetMapping("/expected")
    public ResponseEntity<ApiResponse<ExpectedDividendResponse>> getExpectedDividends(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long stockId) {
        try {
            ExpectedDividendResponse response = dividendQueryService.getExpectedDividends(stockId, userId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (StockNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), "STOCK_NOT_FOUND"));
        } catch (AccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }
}
