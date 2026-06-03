package com.example.dividend.controller;

import com.example.dividend.dto.ApiResponse;
import com.example.dividend.dto.request.GoalCreateRequest;
import com.example.dividend.dto.response.*;
import com.example.dividend.entity.Goal;
import com.example.dividend.repository.GoalRepository;
import com.example.dividend.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final GoalRepository goalRepository;

    /** 목표 달성률 및 예상 소요 기간 */
    @GetMapping("/goal-achievement")
    public ResponseEntity<ApiResponse<GoalAchievementResponse>> getGoalAchievement(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getGoalAchievement(userId)));
    }

    /** 포트폴리오 총 투자금 및 예상 배당 수익 요약 */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalysisSummaryResponse>> getSummary(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getSummary(userId)));
    }

    /** 연도별 배당금 합계 */
    @GetMapping("/annual-dividends")
    public ResponseEntity<ApiResponse<List<AnnualDividendResponse>>> getAnnualDividends(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnnualDividends(userId)));
    }

    /** 종목별 투자 비중 */
    @GetMapping("/stock-weights")
    public ResponseEntity<ApiResponse<List<StockWeightResponse>>> getStockWeights(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getStockWeights(userId)));
    }

    /** 섹터별 투자 비중 */
    @GetMapping("/sector-weights")
    public ResponseEntity<ApiResponse<List<SectorWeightResponse>>> getSectorWeights(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getSectorWeights(userId)));
    }

    /** 연간 목표 배당금 저장 */
    @PostMapping("/goal")
    public ResponseEntity<ApiResponse<Goal>> saveGoal(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid GoalCreateRequest request) {
        int year = LocalDate.now().getYear();
        Goal goal = goalRepository.findByUserIdAndYear(userId, year)
                .orElseGet(Goal::new);
        goal.setUserId(userId);
        goal.setYear(year);
        goal.setTargetDividend(request.getTargetDividend());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(goalRepository.save(goal), "목표가 저장되었습니다"));
    }
}
