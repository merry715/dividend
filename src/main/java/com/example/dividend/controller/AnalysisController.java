package com.example.dividend.controller;

import com.example.dividend.entity.Goal;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final GoalRepository goalRepository;
    private final DashboardService dashboardService;

    public AnalysisController(GoalRepository goalRepository,
                              DashboardService dashboardService) {
        this.goalRepository = goalRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Map<String, Object> analysis() {
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

        Map<String, Object> response = new HashMap<>();
        response.put("targetDividend", targetDividend);
        response.put("totalDividend", totalDividend);
        response.put("achievementRate", String.format("%.1f", achievementRate));
        response.put("monthlyDividends", dashboardService.getMonthlyDividends());
        return response;
    }

    @PostMapping("/goal")
    public Goal saveGoal(@RequestBody Map<String, Object> req) {
        Goal goal = new Goal();
        goal.setTargetDividend(Integer.parseInt(req.get("targetDividend").toString()));
        return goalRepository.save(goal);
    }
}
