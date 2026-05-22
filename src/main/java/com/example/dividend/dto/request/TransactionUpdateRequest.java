package com.example.dividend.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TransactionUpdateRequest {

    @Pattern(regexp = "^(BUY|SELL)$", message = "거래 유형은 BUY 또는 SELL 이어야 합니다")
    private String type;

    @Positive(message = "수량은 양수여야 합니다")
    private Integer quantity;

    @Positive(message = "거래 단가는 양수여야 합니다")
    private Integer price;

    private LocalDate date;

    @PositiveOrZero(message = "수수료는 0 이상이어야 합니다")
    private Integer brokerFee;

    @PositiveOrZero(message = "거래세는 0 이상이어야 합니다")
    private Integer transactionTax;
}
