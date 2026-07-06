package com.storage.warehouse.ledger.excel;

import java.util.Arrays;

public enum MaterialLedgerExcelColumn {
    INDEX(0, "序号"),
    CATEGORY(1, "品类"),
    GENERIC_NAME(2, "统称"),
    BRAND(3, "品牌"),
    NAME(4, "名称"),
    MODEL(5, "型号"),
    BIN_LOCATION(6, "Bin位"),
    STOCK_QUANTITY(7, "库存数量"),
    UNIT_PRICE(8, "单价"),
    REMARK(9, "备注");

    private final int index;
    private final String header;

    MaterialLedgerExcelColumn(int index, String header) {
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
                .map(MaterialLedgerExcelColumn::getHeader)
                .toArray(String[]::new);
    }

    public static int dataColumnCount() {
        return values().length;
    }
}
