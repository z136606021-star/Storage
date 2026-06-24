package com.storage.excel;

import java.util.Arrays;

public enum SysUserExcelColumn {
    NTID(0, "NTID"),
    DISPLAY_NAME(1, "用户姓名"),
    EMAIL(2, "邮箱"),
    PHONE(3, "手机号"),
    ROLE_CODES(4, "角色编码"),
    STATUS(5, "状态");

    private final int index;
    private final String header;

    SysUserExcelColumn(int index, String header) {
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
                .map(SysUserExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
