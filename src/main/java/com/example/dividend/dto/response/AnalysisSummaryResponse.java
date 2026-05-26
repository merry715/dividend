package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AnalysisSummaryResponse {

    private BigDecimal totalInvestment;
    private int totalExpectedDividend;
    private int stockCount;
}
