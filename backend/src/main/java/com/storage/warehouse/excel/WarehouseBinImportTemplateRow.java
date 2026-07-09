package com.storage.warehouse.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class WarehouseBinImportTemplateRow {

    @Excel(name = "排", orderNum = "0")
    private Integer rowNo;

    @Excel(name = "列", orderNum = "1")
    private Integer colNo;

    @Excel(name = "层", orderNum = "2")
    private Integer levelNo;

    @Excel(name = "备注", orderNum = "3")
    private String remark;
}
