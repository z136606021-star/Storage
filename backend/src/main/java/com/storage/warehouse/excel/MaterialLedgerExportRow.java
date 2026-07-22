package com.storage.warehouse.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class MaterialLedgerExportRow {

    @Excel(name = "序号", orderNum = "0")
    private Integer index;

    @Excel(name = "品类", orderNum = "1")
    private String category;

    @Excel(name = "名称", orderNum = "2")
    private String genericName;

    @Excel(name = "品牌", orderNum = "3")
    private String brand;

    @Excel(name = "型号", orderNum = "4")
    private String name;

    @Excel(name = "Bin位", orderNum = "5")
    private String binLocation;

    @Excel(name = "库存数量", orderNum = "6")
    private Integer stockQuantity;

    @Excel(name = "单价", orderNum = "7")
    private String unitPrice;

    @Excel(name = "备注", orderNum = "8")
    private String remark;
}
