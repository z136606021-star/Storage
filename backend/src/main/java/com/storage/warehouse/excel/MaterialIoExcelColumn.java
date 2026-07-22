package com.storage.warehouse.excel;

import java.util.Arrays;

public enum MaterialIoExcelColumn {
    INDEX(0, "序号"),
    CATEGORY(1, "品类"),
    GENERIC_NAME(2, "名称"),
    BRAND(3, "品牌"),
    NAME(4, "型号"),
    BIN_LOCATION(5, "Bin位"),
    QUANTITY(6, "数量"),
    UNIT_PRICE(7, "单价"),
    REMARK(8, "备注"),
    PURPOSE(9, "用途"),
    PROJECT_REF(10, "项目编号"),
    IO_TYPE(11, "操作类型"),
    OPERATOR(12, "操作人"),
    OPERATED_AT(13, "操作时间");

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
        return new String[] {
                "序号", "品类", "统称", "品牌", "名称", "型号", "Bin位", "数量",
                "单价", "备注", "用途", "项目编号", "操作类型", "操作时间"
        };
    }
}
