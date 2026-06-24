package com.storage.excel;

import java.util.Arrays;

public enum SysRoleExcelColumn {
    CODE(0, "角色编码"),
    NAME(1, "角色名称"),
    STATUS(2, "状态"),
    PERMISSIONS(3, "菜单权限标识");

    private final int index;
    private final String header;

    SysRoleExcelColumn(int index, String header) {
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
                .map(SysRoleExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
