package com.storage.warehouse.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.excel.WarehouseBinExcelColumn;
import com.storage.warehouse.excel.WarehouseBinImportTemplateColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseBinExportServiceTest {

    private final WarehouseBinExportService exportService = new WarehouseBinExportServiceImpl();

    @Test
    void export_writesExpectedSheetHeadersAndValues() throws IOException {
        WarehouseBin bin = new WarehouseBin();
        bin.setBinCode("A-01-01");
        bin.setRowNo(1);
        bin.setColNo(2);
        bin.setLevelNo(3);
        bin.setRemark("近门口");

        byte[] bytes = exportService.export(List.of(bin));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Bin位");

            Row headerRow = sheet.getRow(0);
            String[] headers = WarehouseBinExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }

            Row dataRow = sheet.getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBinExcelColumn.INDEX.getIndex()))
                    .isEqualTo("1");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBinExcelColumn.BIN_CODE.getIndex()))
                    .isEqualTo("A-01-01");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBinExcelColumn.ROW_NO.getIndex()))
                    .isEqualTo("1");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBinExcelColumn.COL_NO.getIndex()))
                    .isEqualTo("2");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBinExcelColumn.LEVEL_NO.getIndex()))
                    .isEqualTo("3");
            assertThat(ExcelCellUtils.getCellString(dataRow, WarehouseBinExcelColumn.REMARK.getIndex()))
                    .isEqualTo("近门口");
        }
    }

    @Test
    void exportTemplate_writesImportHeadersOnly() throws IOException {
        byte[] bytes = exportService.exportTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Bin位");

            Row headerRow = sheet.getRow(0);
            String[] headers = WarehouseBinImportTemplateColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
