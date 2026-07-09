package com.storage.warehouse.excel;

import java.util.Arrays;

public enum WarehouseBinImportTemplateColumn {
    ROW_NO(0, "排"),
    COL_NO(1, "列"),
    LEVEL_NO(2, "层"),
    REMARK(3, "备注");

    private final int index;
    private final String header;

    WarehouseBinImportTemplateColumn(int index, String header) {
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
                .map(WarehouseBinImportTemplateColumn::getHeader)
                .toArray(String[]::new);
    }
}
