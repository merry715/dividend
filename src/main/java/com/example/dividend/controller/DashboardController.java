package com.example.dividend.controller;

import com.example.dividend.entity.Goal;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.repository.TransactionRepository;
import com.example.dividend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final TransactionRepository transactionRepository;
    private final DashboardService dashboardService;
    private final GoalRepository goalRepository;

    public DashboardController(TransactionRepository transactionRepository,
                               DashboardService dashboardService,
                               GoalRepository goalRepository) {
        this.transactionRepository = transactionRepository;
        this.dashboardService = dashboardService;
        this.goalRepository = goalRepository;
    }

    @GetMapping
    public Map<String, Object> dashboard() {
        int totalInvestment = transactionRepository.findAll().stream()
                .mapToInt(t -> t.getQuantity() * t.getPrice())
                .sum();

        var holdings = dashboardService.getHoldings();

        int totalDividend = holdings.stream()
                .mapToInt(h -> h.getExpectedDividend())
                .sum();

        int targetDividend = goalRepository.findAll().stream()
                .reduce((first, second) -> second)
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
        response.put("monthlyDividends", dashboardService.getMonthlyDividends());

        return response;
    }
}
