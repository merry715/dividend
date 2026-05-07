package com.example.dividend.controller;

import com.example.dividend.entity.Goal;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AnalysisController {

    private final GoalRepository goalRepository;
    private final DashboardService dashboardService;

    public AnalysisController(GoalRepository goalRepository,
                              DashboardService dashboardService) {
        this.goalRepository = goalRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/analysis")
    public String analysis(Model model) {

        int targetDividend = goalRepository.findAll().stream()
                .reduce((first, second) -> second)
                .map(Goal::getTargetDividend)
                .orElse(0);

        int totalDividend = dashboardService.getHoldings().stream()
                .mapToInt(h -> h.getExpectedDividend())
                .sum();

        double achievementRate = 0;
        if (targetDividend > 0) {
            achievementRate = (double) totalDividend / targetDividend * 100;
        }

        model.addAttribute("targetDividend", targetDividend);
        model.addAttribute("totalDividend", totalDividend);
        model.addAttribute("achievementRate", String.format("%.1f", achievementRate));
        model.addAttribute("monthlyDividends", dashboardService.getMonthlyDividends());

        return "analysis";
    }

    @PostMapping("/analysis/goal")
    public String saveGoal(@RequestParam int targetDividend) {
        Goal goal = new Goal();
        goal.setTargetDividend(targetDividend);
        goalRepository.save(goal);

        return "redirect:/analysis";
    }
}