package com.storage.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseBinSaveDTO {

    @NotBlank(message = "排不能为空")
    @Size(max = 32, message = "排长度不能超过32")
    private String rowNo;

    @Min(value = 1, message = "列必须为正整数")
    private Integer colNo;

    @Min(value = 1, message = "层必须为正整数")
    private Integer levelNo;

    private String remark;
}
