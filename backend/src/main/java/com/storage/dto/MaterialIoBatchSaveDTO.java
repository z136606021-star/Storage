package com.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaterialIoBatchSaveDTO {

    @NotBlank(message = "操作类型不能为空")
    private String ioType;

    private LocalDateTime operatedAt;

    @NotEmpty(message = "请至少添加一行物料")
    @Valid
    private List<MaterialIoBatchItemDTO> items;
}
