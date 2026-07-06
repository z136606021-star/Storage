package com.storage.warehouse.bin.excel;

import java.util.Arrays;

public enum WarehouseBinExcelColumn {
    INDEX(0, "序号"),
    BIN_CODE(1, "Bin位编号"),
    ROW_NO(2, "排"),
    COL_NO(3, "列"),
    LEVEL_NO(4, "层"),
    REMARK(5, "备注");

    private final int index;
    private final String header;

    WarehouseBinExcelColumn(int index, String header) {
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
                .map(WarehouseBinExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
