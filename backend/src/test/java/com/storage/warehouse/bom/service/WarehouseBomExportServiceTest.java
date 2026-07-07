package com.storage.warehouse.bom.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.bom.entity.WarehouseBom;
import com.storage.warehouse.bom.excel.WarehouseBomExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseBomExportServiceTest {

    private final WarehouseBomExportService exportService = new WarehouseBomExportService();

    @Test
    void export_writesExpectedSheetHeadersAndValues() throws IOException {
        WarehouseBom bom = new WarehouseBom();
        bom.setCategory("电子");
        bom.setGenericName("电阻");
        bom.setBrand("YAGEO");
        bom.setName("贴片电阻");
        bom.setRemark("常用料");

        byte[] bytes = exportService.export(List.of(bom));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料清单");

            Row headerRow = sheet.getRow(0);
            String[] headers = WarehouseBomExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }

            Row dataRow = sheet.getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBomExcelColumn.INDEX.getIndex()))
                    .isEqualTo("1");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBomExcelColumn.CATEGORY.getIndex()))
                    .isEqualTo("电子");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBomExcelColumn.GENERIC_NAME.getIndex()))
                    .isEqualTo("电阻");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBomExcelColumn.BRAND.getIndex()))
                    .isEqualTo("YAGEO");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBomExcelColumn.NAME.getIndex()))
                    .isEqualTo("贴片电阻");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBomExcelColumn.REMARK.getIndex()))
                    .isEqualTo("常用料");
        }
    }

    @Test
    void exportTemplate_writesHeaderOnly() throws IOException {
        byte[] bytes = exportService.exportTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料清单");
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
