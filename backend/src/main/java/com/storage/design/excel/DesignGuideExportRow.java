package com.storage.design.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DesignGuideExportRow {

    @ExcelProperty(value = "序号", index = 0)
    private Integer index;

    @ExcelProperty(value = "产品类型编号", index = 1)
    private String productTypeCode;

    @ExcelProperty(value = "产品类型", index = 2)
    private String productTypeName;

    @ExcelProperty(value = "阶段", index = 3)
    private String stageName;

    @ExcelProperty(value = "适用范围", index = 4)
    private String scope;

    @ExcelProperty(value = "检查项", index = 5)
    private String checkItem;

    @ExcelProperty(value = "备注", index = 6)
    private String remark;

    @ExcelProperty(value = "记录人", index = 7)
    private String recorderName;

    @ExcelProperty(value = "记录时间", index = 8)
    private String recordedAt;
}
