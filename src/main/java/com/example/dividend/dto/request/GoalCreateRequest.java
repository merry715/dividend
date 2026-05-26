package com.example.dividend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoalCreateRequest {

    @NotNull(message = "목표 배당금은 필수입니다")
    @Positive(message = "목표 배당금은 양수여야 합니다")
    private Integer targetDividend;
}
