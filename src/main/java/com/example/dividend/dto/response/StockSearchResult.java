package com.example.dividend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StockSearchResult {
    private String ticker;
    private String stockCode;
    private String stockName;
    private String exchange;
    private String currency;
    private String corpCode;   // DART 고유번호 (배당 정보 조회에 사용)
}
