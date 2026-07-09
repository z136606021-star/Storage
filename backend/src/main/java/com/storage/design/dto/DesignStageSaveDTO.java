package com.storage.design.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DesignStageSaveDTO {

    @NotNull(message = "顺序不能为空")
    @Min(value = 1, message = "顺序必须为正整数")
    private Integer sortOrder;

    @NotBlank(message = "阶段不能为空")
    @Size(max = 64, message = "阶段不能超过 64 个字符")
    private String stageName;

    private Boolean enabled = true;
}
