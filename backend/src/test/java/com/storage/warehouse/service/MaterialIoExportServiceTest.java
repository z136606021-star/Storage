package com.storage.warehouse.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.dto.MaterialIoRecordVO;
import com.storage.warehouse.excel.MaterialIoExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialIoExportServiceTest {

    private final MaterialIoExportService exportService = new MaterialIoExportServiceImpl();

    @Test
    void export_writesExpectedSheetHeadersAndValues() throws IOException {
        MaterialIoRecordVO record = new MaterialIoRecordVO();
        record.setCategory("电子");
        record.setGenericName("电阻");
        record.setBrand("YAGEO");
        record.setName("贴片电阻");
        record.setModel("0805-10K");
        record.setBinLocation("A-01");
        record.setQuantity(5);
        record.setPurpose("EMPLOYEE_PICKUP");
        record.setRemark("测试备注");
        record.setIoType("OUT");
        record.setOperatorDisplayName("张三");
        record.setOperatedAt(LocalDateTime.of(2026, 7, 6, 10, 30, 0));

        byte[] bytes = exportService.export(List.of(record));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料出入库");

            Row headerRow = sheet.getRow(0);
            String[] headers = MaterialIoExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }

            Row dataRow = sheet.getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.INDEX.getIndex()))
                    .isEqualTo("1");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.CATEGORY.getIndex()))
                    .isEqualTo("电子");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.GENERIC_NAME.getIndex()))
                    .isEqualTo("电阻");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.BRAND.getIndex()))
                    .isEqualTo("YAGEO");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.NAME.getIndex()))
                    .isEqualTo("贴片电阻");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.MODEL.getIndex()))
                    .isEqualTo("0805-10K");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.BIN_LOCATION.getIndex()))
                    .isEqualTo("A-01");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.QUANTITY.getIndex()))
                    .isEqualTo("5");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.PURPOSE.getIndex()))
                    .isEqualTo("员工领用");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.REMARK.getIndex()))
                    .isEqualTo("测试备注");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.IO_TYPE.getIndex()))
                    .isEqualTo("出库");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.OPERATOR.getIndex()))
                    .isEqualTo("张三");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.OPERATED_AT.getIndex()))
                    .isEqualTo("2026-07-06 10:30:00");
        }
    }

    @Test
    void export_withUsernameFallback_usesOperatorUsername() throws IOException {
        MaterialIoRecordVO record = new MaterialIoRecordVO();
        record.setCategory("电子");
        record.setGenericName("电阻");
        record.setBrand("YAGEO");
        record.setName("贴片电阻");
        record.setModel("0805-10K");
        record.setBinLocation("A-01");
        record.setQuantity(1);
        record.setIoType("IN");
        record.setOperatorUsername("admin");

        byte[] bytes = exportService.export(List.of(record));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.OPERATOR.getIndex()))
                    .isEqualTo("admin");
            assertThat(ExcelCellUtils.getCellString(dataRow, MaterialIoExcelColumn.IO_TYPE.getIndex()))
                    .isEqualTo("入库");
        }
    }

    @Test
    void exportImportTemplate_writesExpectedHeadersWithoutOperator() throws IOException {
        byte[] bytes = exportService.exportImportTemplate();

        assertImportTemplateContent(bytes);
    }

    @Test
    void exportTemplate_writesSameImportTemplateContent() throws IOException {
        byte[] templateBytes = exportService.exportTemplate();
        byte[] importTemplateBytes = exportService.exportImportTemplate();

        assertImportTemplateContent(templateBytes);
        assertImportTemplateContent(importTemplateBytes);
    }

    private static void assertImportTemplateContent(byte[] bytes) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("物料出入库导入");
            assertThat(sheet.getRow(1)).isNull();

            Row headerRow = sheet.getRow(0);
            String[] templateHeaders = MaterialIoExcelColumn.importTemplateHeaders();
            for (int i = 0; i < templateHeaders.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(templateHeaders[i]);
            }
            assertThat(Arrays.asList(templateHeaders)).doesNotContain("操作人");
        }
    }
}
