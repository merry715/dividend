package com.example.dividend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SectorWeightResponse {

    private String sectorCode;
    private String sectorLabel;
    private BigDecimal investment;
    private BigDecimal weightPercent;
}
