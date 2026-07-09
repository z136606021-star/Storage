package com.storage.design.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DesignProductTypeExportRow {

    @ExcelProperty(value = "NO.", index = 0)
    private Integer index;

    @ExcelProperty(value = "产品类型编号", index = 1)
    private String typeCode;

    @ExcelProperty(value = "产品类型", index = 2)
    private String typeName;

    @ExcelProperty(value = "是否启用", index = 3)
    private String enabled;

    @ExcelProperty(value = "操作人", index = 4)
    private String operatorName;

    @ExcelProperty(value = "操作日期", index = 5)
    private String operatedAt;
}
