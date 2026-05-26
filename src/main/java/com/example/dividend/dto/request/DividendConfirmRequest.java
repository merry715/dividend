package com.example.dividend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DividendConfirmRequest {

    @NotNull(message = "확정 배당금은 필수입니다")
    @Positive(message = "확정 배당금은 양수여야 합니다")
    private Long confirmedAmount;

    @NotNull(message = "지급일은 필수입니다")
    private LocalDate paymentDate;
}
