package com.example.dividend.controller;

import com.example.dividend.entity.Goal;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final GoalRepository goalRepository;

    public DashboardController(DashboardService dashboardService,
                               GoalRepository goalRepository) {
        this.dashboardService = dashboardService;
        this.goalRepository = goalRepository;
    }

    @GetMapping
    public Map<String, Object> dashboard(@AuthenticationPrincipal Long userId) {
        var holdings = dashboardService.getHoldings(userId);

        // totalInvestment: 해당 사용자의 순 투자금 (Transaction 집계 기반, SELL 차감)
        long totalInvestment = holdings.stream()
                .mapToLong(h -> Math.max(h.getTotalInvestment(), 0L))
                .sum();

        int totalDividend = holdings.stream()
                .mapToInt(h -> h.getExpectedDividend())
                .sum();

        int currentYear = LocalDate.now().getYear();
        int targetDividend = goalRepository.findByUserIdAndYear(userId, currentYear)
                .map(Goal::getTargetDividend)
                .orElse(0);

        double achievementRate = 0;
        if (targetDividend > 0) {
            achievementRate = (double) totalDividend / targetDividend * 100;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalInvestment", totalInvestment);
        response.put("holdings", holdings);
        response.put("totalDividend", totalDividend);
        response.put("targetDividend", targetDividend);
        response.put("achievementRate", String.format("%.1f", achievementRate));
        response.put("monthlyDividends", dashboardService.getMonthlyDividends(userId));

        return response;
    }
}
