package com.storage.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WarehouseBinSaveDTO {

    @NotNull(message = "排不能为空")
    @Min(value = 1, message = "排必须为正整数")
    private Integer rowNo;

    @NotNull(message = "列不能为空")
    @Min(value = 1, message = "列必须为正整数")
    private Integer colNo;

    @NotNull(message = "层不能为空")
    @Min(value = 1, message = "层必须为正整数")
    private Integer levelNo;

    private String remark;
}
