package com.example.dividend.dto.request;

import com.example.dividend.entity.StockSector;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SectorUpdateRequest {

    @NotNull(message = "섹터는 필수입니다")
    private StockSector sector;
}
