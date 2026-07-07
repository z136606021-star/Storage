package com.storage.system.user.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.system.user.dto.SysUserVO;
import com.storage.system.user.excel.SysUserExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SysUserExportServiceTest {

    private final SysUserExportService exportService = new SysUserExportServiceImpl();

    @Test
    void export_writesExpectedSheetHeadersAndValues() throws IOException {
        SysUserVO user = SysUserVO.builder()
                .username("alice")
                .displayName("Alice")
                .email("alice@example.com")
                .phone("13800000000")
                .roleCodes(List.of("ADMIN", "USER"))
                .status(1)
                .build();

        byte[] bytes = exportService.export(List.of(user));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("用户");

            Row headerRow = sheet.getRow(0);
            String[] headers = SysUserExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }

            Row dataRow = sheet.getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, SysUserExcelColumn.NTID.getIndex()))
                    .isEqualTo("alice");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysUserExcelColumn.DISPLAY_NAME.getIndex()))
                    .isEqualTo("Alice");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysUserExcelColumn.EMAIL.getIndex()))
                    .isEqualTo("alice@example.com");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysUserExcelColumn.PHONE.getIndex()))
                    .isEqualTo("13800000000");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysUserExcelColumn.ROLE_CODES.getIndex()))
                    .isEqualTo("ADMIN,USER");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysUserExcelColumn.STATUS.getIndex()))
                    .isEqualTo("启用");
        }
    }

    @Test
    void export_withDisabledStatus_writesDisabledLabel() throws IOException {
        SysUserVO user = SysUserVO.builder()
                .username("bob")
                .displayName("Bob")
                .status(0)
                .build();

        byte[] bytes = exportService.export(List.of(user));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, SysUserExcelColumn.STATUS.getIndex()))
                    .isEqualTo("停用");
        }
    }

    @Test
    void exportTemplate_writesHeaderOnly() throws IOException {
        byte[] bytes = exportService.exportTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("用户");
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
