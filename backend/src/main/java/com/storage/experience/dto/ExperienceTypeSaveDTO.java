package com.storage.experience.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExperienceTypeSaveDTO {

    @NotBlank(message = "类型名称不能为空")
    @Size(max = 64, message = "类型名称不能超过64个字符")
    private String name;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Integer sortOrder;
}
