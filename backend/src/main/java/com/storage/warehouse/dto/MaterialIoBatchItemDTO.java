package com.storage.warehouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialIoBatchItemDTO {

    private Long materialLedgerId;

    private Long bomId;

    @Size(max = 32, message = "Bin位长度不能超过32")
    private String binLocation;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;

    @DecimalMin(value = "0", message = "单价不能为负数")
    @Digits(integer = 8, fraction = 2, message = "单价格式不正确")
    private BigDecimal unitPrice;

    private String remark;

    private String purpose;

    private String projectRef;
}
