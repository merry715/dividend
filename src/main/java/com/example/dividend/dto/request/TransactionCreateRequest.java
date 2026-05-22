package com.example.dividend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TransactionCreateRequest {

    @NotNull(message = "종목 ID는 필수입니다")
    private Long stockId;

    @NotBlank(message = "거래 유형은 필수입니다")
    @Pattern(regexp = "^(BUY|SELL)$", message = "거래 유형은 BUY 또는 SELL 이어야 합니다")
    private String type;

    @NotNull(message = "수량은 필수입니다")
    @Positive(message = "수량은 양수여야 합니다")
    private Integer quantity;

    @NotNull(message = "거래 단가는 필수입니다")
    @Positive(message = "거래 단가는 양수여야 합니다")
    private Integer price;

    @NotNull(message = "거래일은 필수입니다")
    private LocalDate date;

    @NotNull(message = "수수료는 필수입니다")
    @PositiveOrZero(message = "수수료는 0 이상이어야 합니다")
    private Integer brokerFee;

    @NotNull(message = "거래세는 필수입니다")
    @PositiveOrZero(message = "거래세는 0 이상이어야 합니다")
    private Integer transactionTax;
}
