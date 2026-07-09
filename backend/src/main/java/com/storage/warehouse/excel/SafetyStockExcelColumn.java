package com.storage.warehouse.excel;

import java.util.Arrays;

public enum SafetyStockExcelColumn {
    INDEX(0, "序号"),
    CATEGORY(1, "品类"),
    GENERIC_NAME(2, "统称"),
    BRAND(3, "品牌"),
    NAME(4, "名称"),
    MODEL(5, "型号"),
    BIN_LOCATION(6, "Bin位"),
    STOCK_QUANTITY(7, "库存总数"),
    SAFETY_QUANTITY(8, "安全库存数"),
    WARNING_PERIOD(9, "库存预警");

    private final int index;
    private final String header;

    SafetyStockExcelColumn(int index, String header) {
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
                .map(SafetyStockExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
