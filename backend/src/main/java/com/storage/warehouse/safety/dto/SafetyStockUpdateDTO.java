package com.storage.warehouse.safety.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SafetyStockUpdateDTO {

    @NotNull
    @Min(0)
    private Integer safetyQuantity;

    @NotNull
    private Boolean warningEnabled;
}
