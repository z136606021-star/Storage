package com.storage.warehouse.excel;

import java.util.Arrays;

public enum WarehouseBomImportTemplateColumn {
    CATEGORY(0, "品类"),
    GENERIC_NAME(1, "统称"),
    BRAND(2, "品牌"),
    NAME(3, "名称"),
    REMARK(4, "备注");

    private final int index;
    private final String header;

    WarehouseBomImportTemplateColumn(int index, String header) {
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
                .map(WarehouseBomImportTemplateColumn::getHeader)
                .toArray(String[]::new);
    }
}
