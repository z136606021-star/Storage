package com.storage.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialSaveDTO {

    @NotBlank(message = "品类不能为空")
    @Size(max = 64, message = "品类长度不能超过64")
    private String category;

    @NotBlank(message = "统称不能为空")
    @Size(max = 64, message = "统称长度不能超过64")
    private String genericName;

    @Size(max = 64, message = "品牌长度不能超过64")
    private String brand;

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称长度不能超过128")
    private String name;

    @Size(max = 64, message = "型号长度不能超过64")
    private String model;

    @NotBlank(message = "Bin位不能为空")
    @Size(max = 32, message = "Bin位长度不能超过32")
    private String binLocation;

    private BigDecimal unitPrice;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
