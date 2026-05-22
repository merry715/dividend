package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.request.GoalCreateRequest;
import com.example.dividend.entity.Goal;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final GoalRepository goalRepository;
    private final DashboardService dashboardService;

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
    public ResponseEntity<ApiResponse<Goal>> saveGoal(
            @RequestBody @Valid GoalCreateRequest request) {
        Goal goal = new Goal();
        goal.setTargetDividend(request.getTargetDividend());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(goalRepository.save(goal), "목표가 저장되었습니다"));
    }
}
