package com.storage.warehouse.ledger.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.excel.MaterialLedgerExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialLedgerExportServiceTest {

    private final MaterialLedgerExportService exportService = new MaterialLedgerExportServiceImpl();

    @Test
    void export_writesExpectedSheetHeadersAndValues() throws IOException {
        MaterialLedger ledger = new MaterialLedger();
        ledger.setCategory("电子");
        ledger.setGenericName("电阻");
        ledger.setBrand("YAGEO");
        ledger.setName("贴片电阻");
        ledger.setModel("0805-10K");
        ledger.setBinLocation("A-01");
        ledger.setStockQuantity(12);
        ledger.setUnitPrice(new BigDecimal("1.235"));
        ledger.setRemark("测试备注");

        byte[] bytes = exportService.export(List.of(ledger));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料台账");

            Row headerRow = sheet.getRow(0);
            String[] headers = MaterialLedgerExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }

            Row dataRow = sheet.getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.INDEX.getIndex()))
                    .isEqualTo("1");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.CATEGORY.getIndex()))
                    .isEqualTo("电子");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.GENERIC_NAME.getIndex()))
                    .isEqualTo("电阻");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.BRAND.getIndex()))
                    .isEqualTo("YAGEO");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.NAME.getIndex()))
                    .isEqualTo("贴片电阻");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.MODEL.getIndex()))
                    .isEqualTo("0805-10K");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.BIN_LOCATION.getIndex()))
                    .isEqualTo("A-01");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.STOCK_QUANTITY.getIndex()))
                    .isEqualTo("12");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.UNIT_PRICE.getIndex()))
                    .isEqualTo("1.24");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.REMARK.getIndex()))
                    .isEqualTo("测试备注");
        }
    }

    @Test
    void export_withNullUnitPrice_writesEmptyPriceCell() throws IOException {
        MaterialLedger ledger = new MaterialLedger();
        ledger.setCategory("电子");
        ledger.setGenericName("电阻");
        ledger.setBrand("YAGEO");
        ledger.setName("贴片电阻");
        ledger.setModel("0805-10K");
        ledger.setBinLocation("A-01");
        ledger.setStockQuantity(0);
        ledger.setUnitPrice(null);

        byte[] bytes = exportService.export(List.of(ledger));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialLedgerExcelColumn.UNIT_PRICE.getIndex()))
                    .isNull();
        }
    }

    @Test
    void exportTemplate_writesHeaderOnly() throws IOException {
        byte[] bytes = exportService.exportTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料台账");
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
