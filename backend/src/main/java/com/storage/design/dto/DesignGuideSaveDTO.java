package com.storage.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DesignGuideSaveDTO {

    @NotNull(message = "产品类型不能为空")
    private Long productTypeId;

    @NotNull(message = "项目阶段不能为空")
    private Long stageId;

    @NotBlank(message = "适用范围不能为空")
    @Size(max = 64, message = "适用范围不能超过 64 个字符")
    private String scope;

    @NotBlank(message = "检查项不能为空")
    @Size(max = 500, message = "检查项不能超过 500 个字符")
    private String checkItem;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
