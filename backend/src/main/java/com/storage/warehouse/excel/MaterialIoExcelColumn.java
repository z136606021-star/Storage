package com.storage.warehouse.excel;

import java.util.Arrays;

public enum MaterialIoExcelColumn {
    INDEX(0, "序号"),
    CATEGORY(1, "品类"),
    GENERIC_NAME(2, "统称"),
    BRAND(3, "品牌"),
    NAME(4, "名称"),
    MODEL(5, "型号"),
    BIN_LOCATION(6, "Bin位"),
    QUANTITY(7, "数量"),
    PURPOSE(8, "用途"),
    REMARK(9, "备注"),
    IO_TYPE(10, "操作类型"),
    OPERATOR(11, "操作人"),
    OPERATED_AT(12, "操作时间");

    private final int index;
    private final String header;

    MaterialIoExcelColumn(int index, String header) {
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
                .map(MaterialIoExcelColumn::getHeader)
                .toArray(String[]::new);
    }

    public static String[] importTemplateHeaders() {
        return Arrays.stream(values())
                .filter(column -> column != OPERATOR)
                .map(MaterialIoExcelColumn::getHeader)
                .toArray(String[]::new);
    }
}
