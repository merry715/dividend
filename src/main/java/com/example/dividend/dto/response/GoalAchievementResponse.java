package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoalAchievementResponse {

    private int targetDividend;
    private int currentDividend;
    private double achievementRate;
    private Integer estimatedMonthsToGoal;
}
