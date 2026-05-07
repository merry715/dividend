package com.example.dividend.controller;

import com.example.dividend.entity.Goal;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.repository.TransactionRepository;
import com.example.dividend.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
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

    @GetMapping("/")
    public String dashboard(Model model) {

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

        model.addAttribute("totalInvestment", totalInvestment);
        model.addAttribute("holdings", holdings);
        model.addAttribute("totalDividend", totalDividend);
        model.addAttribute("targetDividend", targetDividend);
        model.addAttribute("achievementRate", String.format("%.1f", achievementRate));
        model.addAttribute("monthlyDividends", dashboardService.getMonthlyDividends());

        return "dashboard";
    }
}