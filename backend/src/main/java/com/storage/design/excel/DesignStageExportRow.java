package com.storage.design.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DesignStageExportRow {

    @ExcelProperty(value = "顺序", index = 0)
    private Integer sortOrder;

    @ExcelProperty(value = "阶段", index = 1)
    private String stageName;

    @ExcelProperty(value = "是否启用", index = 2)
    private String enabled;

    @ExcelProperty(value = "操作人", index = 3)
    private String operatorName;

    @ExcelProperty(value = "操作日期", index = 4)
    private String operatedAt;
}
