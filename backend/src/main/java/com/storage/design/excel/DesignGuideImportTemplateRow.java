package com.storage.design.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DesignGuideImportTemplateRow {

    @ExcelProperty(value = "产品类型编号", index = 0)
    private String productTypeCode;

    @ExcelProperty(value = "产品类型", index = 1)
    private String productTypeName;

    @ExcelProperty(value = "阶段", index = 2)
    private String stageName;

    @ExcelProperty(value = "适用范围", index = 3)
    private String scope;

    @ExcelProperty(value = "检查项", index = 4)
    private String checkItem;

    @ExcelProperty(value = "备注", index = 5)
    private String remark;
}
