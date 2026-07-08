package com.storage.warehouse.excel;

import java.util.Arrays;

/**
 * 物料出入库 Excel 导入模板列序 SSOT（不含「操作人」，与 {@link MaterialIoImportTemplateRow} 一致）。
 */
public enum MaterialIoImportTemplateColumn {
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
    OPERATED_AT(11, "操作时间");

    private final int index;
    private final String header;

    MaterialIoImportTemplateColumn(int index, String header) {
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
                .map(MaterialIoImportTemplateColumn::getHeader)
                .toArray(String[]::new);
    }
}
