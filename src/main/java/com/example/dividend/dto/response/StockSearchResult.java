package com.example.dividend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StockSearchResult {
    private String ticker;
    private String stockCode;
    private String stockName;
    private String exchange;
    private String currency;
}
