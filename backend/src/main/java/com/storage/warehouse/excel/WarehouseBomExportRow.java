package com.storage.warehouse.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class WarehouseBomExportRow {

    @Excel(name = "序号", orderNum = "0")
    private Integer index;

    @Excel(name = "品类", orderNum = "1")
    private String category;

    @Excel(name = "统称", orderNum = "2")
    private String genericName;

    @Excel(name = "品牌", orderNum = "3")
    private String brand;

    @Excel(name = "名称", orderNum = "4")
    private String name;

    @Excel(name = "规格", orderNum = "5")
    private String model;

    @Excel(name = "备注", orderNum = "6")
    private String remark;
}
