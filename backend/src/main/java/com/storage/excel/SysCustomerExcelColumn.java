package com.storage.excel;

import java.util.Arrays;

public enum SysCustomerExcelColumn {
    INDEX(0, "序号"),
    CUSTOMER_CODE(1, "客户编号"),
    NAME(2, "客户名称"),
    CONTACT_NAME(3, "联系人"),
    PHONE(4, "电话"),
    EMAIL(5, "邮箱"),
    ADDRESS(6, "地址"),
    STATUS(7, "状态"),
    REMARK(8, "备注");

    private final int index;
    private final String header;

    SysCustomerExcelColumn(int index, String header) {
        this.index = index;
        this.header = header;
    }

    public int getIndex() {
        return index;
    }

    public String getHeader() {
        return header;
    }

    public static String[] headers() {
        return Arrays.stream(values())
                .map(SysCustomerExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
