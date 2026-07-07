package com.storage.warehouse.bin.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class WarehouseBinExportRow {

    @Excel(name = "序号", orderNum = "0")
    private Integer index;

    @Excel(name = "Bin位编号", orderNum = "1")
    private String binCode;

    @Excel(name = "排", orderNum = "2")
    private Integer rowNo;

    @Excel(name = "列", orderNum = "3")
    private Integer colNo;

    @Excel(name = "层", orderNum = "4")
    private Integer levelNo;

    @Excel(name = "备注", orderNum = "5")
    private String remark;
}
