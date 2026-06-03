package com.example.dividend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class StockDividendResponse {
    private Long stockId;
    private String stockName;
    private String dividendCycle;
    private Integer dividendPerShare;
    private List<Map<String, Object>> paymentDates;
    private String stockStatus;
    private Long totalExpectedAmount;
}
