package com.storage.warehouse.excel;

import java.util.Arrays;

/**
 * 物料出入库 Excel 导入模板列序 SSOT（不含「操作人」，与 {@link MaterialIoImportTemplateRow} 一致）。
 */
public enum MaterialIoImportTemplateColumn {
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
    OPERATED_AT(12, "操作时间");

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
