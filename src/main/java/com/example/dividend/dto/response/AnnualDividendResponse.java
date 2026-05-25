package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnualDividendResponse {

    private int year;
    private int totalDividend;
}
