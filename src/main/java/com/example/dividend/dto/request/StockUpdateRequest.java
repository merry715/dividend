package com.example.dividend.dto.request;

import com.example.dividend.entity.StockSector;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class StockUpdateRequest {

    @Size(max = 100, message = "종목명은 100자 이하여야 합니다")
    private String stockName;

    private StockSector sector;

    @Pattern(regexp = "^(KOSPI|KRX|KOSDAQ)$", message = "거래소는 KOSPI(KRX) 또는 KOSDAQ만 허용됩니다")
    private String exchange;

    @PositiveOrZero(message = "보유 수량은 0 이상이어야 합니다")
    private Integer quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "평균 단가는 0 이상이어야 합니다")
    private BigDecimal avgPrice;

    /** 배당 주기 — ANNUAL / QUARTERLY / MONTHLY */
    @Pattern(regexp = "^(ANNUAL|QUARTERLY|MONTHLY)$",
             message = "배당 주기는 ANNUAL, QUARTERLY, MONTHLY 중 하나여야 합니다")
    private String dividendCycle;

    /** 주당 예상 배당금 */
    @PositiveOrZero(message = "주당 예상 배당금은 0 이상이어야 합니다")
    private Integer expectedDividendPerShare;
}
