package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> getUserStats() {
        return ApiResponse.ok(adminStatsService.getUserStats());
    }

    @GetMapping("/users/active")
    public ApiResponse<Map<String, Object>> getActiveUsers() {
        return ApiResponse.ok(adminStatsService.getActiveUsers());
    }

    @GetMapping("/stocks/top10")
    public ApiResponse<List<Map<String, Object>>> getTop10Stocks() {
        return ApiResponse.ok(adminStatsService.getTop10Stocks());
    }

    @GetMapping("/sectors")
    public ApiResponse<List<Map<String, Object>>> getSectorDistribution() {
        return ApiResponse.ok(adminStatsService.getSectorDistribution());
    }

    @GetMapping("/dividends/average")
    public ApiResponse<Map<String, Object>> getDividendAverages() {
        return ApiResponse.ok(adminStatsService.getDividendAverages());
    }
}
