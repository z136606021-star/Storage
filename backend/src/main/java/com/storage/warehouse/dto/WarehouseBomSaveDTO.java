package com.storage.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseBomSaveDTO {

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

    @NotBlank(message = "型号不能为空")
    @Size(max = 64, message = "型号长度不能超过64")
    private String model;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    @Size(max = 512, message = "图片对象键长度不能超过512")
    private String imageObjectKey;
}
