package com.storage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialIoSaveDTO {

    @NotNull(message = "请选择物料台账")
    private Long materialLedgerId;

    @NotBlank(message = "操作类型不能为空")
    private String ioType;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;

    private String remark;

    private String purpose;

    private String projectRef;

    /** 导入解析用，可选操作时间 */
    private LocalDateTime operatedAt;

    /** 导入解析用，API 写入时不使用 */
    private String category;
    private String genericName;
    private String brand;
    private String name;
    private String model;
    private String binLocation;
}
