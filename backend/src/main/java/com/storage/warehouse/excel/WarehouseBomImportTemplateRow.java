package com.storage.warehouse.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class WarehouseBomImportTemplateRow {

    @Excel(name = "品类", orderNum = "0")
    private String category;

    @Excel(name = "统称", orderNum = "1")
    private String genericName;

    @Excel(name = "品牌", orderNum = "2")
    private String brand;

    @Excel(name = "名称", orderNum = "3")
    private String name;

    @Excel(name = "备注", orderNum = "4")
    private String remark;
}
