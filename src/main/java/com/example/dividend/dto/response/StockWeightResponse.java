package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class StockWeightResponse {

    private Long stockId;
    private String stockName;
    private String stockCode;
    private BigDecimal investment;
    private BigDecimal weightPercent;
}
