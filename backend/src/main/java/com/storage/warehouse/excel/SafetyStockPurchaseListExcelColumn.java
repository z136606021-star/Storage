package com.storage.warehouse.excel;

import java.util.Arrays;

public enum SafetyStockPurchaseListExcelColumn {
    INDEX(0, "序号"),
    CATEGORY(1, "品类"),
    GENERIC_NAME(2, "名称"),
    BRAND(3, "品牌"),
    NAME(4, "型号"),
    BIN_LOCATION(5, "Bin位"),
    STOCK_QUANTITY(6, "库存总数"),
    SAFETY_QUANTITY(7, "安全库存数"),
    SUGGESTED_PURCHASE_QUANTITY(8, "建议采购数"),
    REMARK(9, "备注");

    private final int index;
    private final String header;

    SafetyStockPurchaseListExcelColumn(int index, String header) {
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
                .map(SafetyStockPurchaseListExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
