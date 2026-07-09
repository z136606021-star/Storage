package com.storage.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DesignProductTypeSaveDTO {

    @NotBlank(message = "产品类型编号不能为空")
    @Size(max = 32, message = "产品类型编号不能超过 32 个字符")
    private String typeCode;

    @NotBlank(message = "产品类型不能为空")
    @Size(max = 64, message = "产品类型不能超过 64 个字符")
    private String typeName;

    private Boolean enabled = true;
}
