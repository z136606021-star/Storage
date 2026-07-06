package com.storage.warehouse.bom.excel;

import java.util.Arrays;

public enum WarehouseBomExcelColumn {
    INDEX(0, "序号"),
    CATEGORY(1, "品类"),
    GENERIC_NAME(2, "统称"),
    BRAND(3, "品牌"),
    NAME(4, "名称"),
    REMARK(5, "备注");

    private final int index;
    private final String header;

    WarehouseBomExcelColumn(int index, String header) {
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
                .map(WarehouseBomExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
