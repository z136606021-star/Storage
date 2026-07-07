package com.storage.warehouse.safety.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.safety.dto.SafetyStockRecordVO;
import com.storage.warehouse.safety.excel.SafetyStockExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyStockExportServiceTest {

  private final SafetyStockExportService exportService = new SafetyStockExportService();

  @Test
  void export_writesExpectedSheetHeadersAndValues() throws IOException {
    SafetyStockRecordVO inWarning = sampleRecord(true);
    SafetyStockRecordVO normal = sampleRecord(false);
    normal.setCategory("机械");
    normal.setGenericName("轴承");
    normal.setBrand("SKF");
    normal.setName("深沟球轴承");
    normal.setModel("6205");
    normal.setBinLocation("B-02");
    normal.setStockQuantity(20);
    normal.setSafetyQuantity(10);

    byte[] bytes = exportService.export(List.of(inWarning, normal));

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("安全库存");
      assertThat(workbook.getNumberOfSheets()).isEqualTo(1);

      Row headerRow = sheet.getRow(0);
      String[] headers = SafetyStockExcelColumn.headers();
      for (int i = 0; i < headers.length; i++) {
        assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
      }

      Row firstDataRow = sheet.getRow(1);
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.INDEX.getIndex()))
          .isEqualTo("1");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.CATEGORY.getIndex()))
          .isEqualTo("电子");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.GENERIC_NAME.getIndex()))
          .isEqualTo("电阻");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.BRAND.getIndex()))
          .isEqualTo("YAGEO");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.NAME.getIndex()))
          .isEqualTo("贴片电阻");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.MODEL.getIndex()))
          .isEqualTo("0805-10K");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.BIN_LOCATION.getIndex()))
          .isEqualTo("A-01");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.STOCK_QUANTITY.getIndex()))
          .isEqualTo("5");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.SAFETY_QUANTITY.getIndex()))
          .isEqualTo("10");
      assertThat(ExcelCellUtils.getCellString(firstDataRow, SafetyStockExcelColumn.WARNING_PERIOD.getIndex()))
          .isEqualTo("是");

      Row secondDataRow = sheet.getRow(2);
      assertThat(ExcelCellUtils.getCellString(secondDataRow, SafetyStockExcelColumn.INDEX.getIndex()))
          .isEqualTo("2");
      assertThat(ExcelCellUtils.getCellString(secondDataRow, SafetyStockExcelColumn.WARNING_PERIOD.getIndex()))
          .isEqualTo("否");
      assertThat(sheet.getRow(3)).isNull();
    }
  }

  @Test
  void export_withEmptyList_writesHeaderOnly() throws IOException {
    byte[] bytes = exportService.export(List.of());

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      assertThat(sheet.getSheetName()).isEqualTo("安全库存");
      assertThat(sheet.getRow(0)).isNotNull();
      assertThat(sheet.getRow(1)).isNull();
    }
  }

  private static SafetyStockRecordVO sampleRecord(boolean inWarningPeriod) {
    SafetyStockRecordVO record = new SafetyStockRecordVO();
    record.setCategory("电子");
    record.setGenericName("电阻");
    record.setBrand("YAGEO");
    record.setName("贴片电阻");
    record.setModel("0805-10K");
    record.setBinLocation("A-01");
    record.setStockQuantity(5);
    record.setSafetyQuantity(10);
    record.setInWarningPeriod(inWarningPeriod);
    return record;
  }
}
