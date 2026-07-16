package com.storage.warehouse.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

import java.math.BigDecimal;

@Data
public class MaterialIoExportRow {

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

    @Excel(name = "型号", orderNum = "5")
    private String model;

    @Excel(name = "Bin位", orderNum = "6")
    private String binLocation;

    @Excel(name = "数量", orderNum = "7")
    private Integer quantity;

    @Excel(name = "单价", orderNum = "8")
    private BigDecimal unitPrice;

    @Excel(name = "备注", orderNum = "9")
    private String remark;

    @Excel(name = "用途", orderNum = "10")
    private String purpose;

    @Excel(name = "项目编号", orderNum = "11")
    private String projectRef;

    @Excel(name = "操作类型", orderNum = "12")
    private String ioType;

    @Excel(name = "操作人", orderNum = "13")
    private String operator;

    @Excel(name = "操作时间", orderNum = "14")
    private String operatedAt;
}
