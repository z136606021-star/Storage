package com.storage.warehouse.io.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MaterialIoBatchItemDTO {

    private Long materialLedgerId;

    private Long bomId;

    @Size(max = 32, message = "Bin位长度不能超过32")
    private String binLocation;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;

    private String remark;

    private String purpose;

    private String projectRef;
}
