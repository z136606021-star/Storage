package com.storage.design.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class DesignGuideImportRow {

    @Excel(name = "产品类型编号", orderNum = "0")
    private String productTypeCode;

    @Excel(name = "产品类型", orderNum = "1")
    private String productTypeName;

    @Excel(name = "阶段", orderNum = "2")
    private String stageName;

    @Excel(name = "适用范围", orderNum = "3")
    private String scope;

    @Excel(name = "检查项", orderNum = "4")
    private String checkItem;

    @Excel(name = "备注", orderNum = "5")
    private String remark;
}
