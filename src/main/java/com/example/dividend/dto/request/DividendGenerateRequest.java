package com.example.dividend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DividendGenerateRequest {

    @NotNull(message = "종목 ID는 필수입니다")
    private Long stockId;

    @Min(value = 2000, message = "연도는 2000 이상이어야 합니다")
    @Max(value = 2100, message = "연도는 2100 이하여야 합니다")
    private Integer year;  // null이면 현재 연도 사용
}
