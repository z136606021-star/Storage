package com.storage.system.customer.excel;

import lombok.Data;
import org.jeecgframework.poi.excel.annotation.Excel;

@Data
public class SysCustomerExportRow {

    @Excel(name = "序号", orderNum = "0")
    private Integer index;

    @Excel(name = "客户编号", orderNum = "1")
    private String customerCode;

    @Excel(name = "客户名称", orderNum = "2")
    private String name;

    @Excel(name = "联系人", orderNum = "3")
    private String contactName;

    @Excel(name = "电话", orderNum = "4")
    private String phone;

    @Excel(name = "邮箱", orderNum = "5")
    private String email;

    @Excel(name = "地址", orderNum = "6")
    private String address;

    @Excel(name = "状态", orderNum = "7")
    private String status;

    @Excel(name = "备注", orderNum = "8")
    private String remark;
}
