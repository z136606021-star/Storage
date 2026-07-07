package com.storage.common.excel;

import com.storage.common.dto.ImportResultVO;
import lombok.Data;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AutoPoiExcelTemplateTest {

    @Test
    void parseRows_keepsOriginalExcelRowNumberWhenBlankRowsAreSkipped() throws IOException {
        MockMultipartFile file = createExcelWithBlankMiddleRow();

        AutoPoiExcelTemplate.ParsedRows<String> parsed = AutoPoiExcelTemplate.parseRows(
                file,
                SimpleImportRow.class,
                row -> !StringUtils.hasText(row.getName()),
                (excelRow, row) -> {
                    if ("bad".equals(row.getName())) {
                        throw new IllegalArgumentException("名称不合法");
                    }
                    return row.getName();
                }
        );

        assertThat(parsed.rows()).extracting(AutoPoiExcelTemplate.ParsedRow::excelRow).containsExactly(2);
        assertThat(parsed.errors()).extracting(ImportResultVO.ImportErrorVO::getRow).containsExactly(4);
    }

    private MockMultipartFile createExcelWithBlankMiddleRow() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("名称");
            sheet.createRow(1).createCell(0).setCellValue("ok");
            sheet.createRow(2);
            sheet.createRow(3).createCell(0).setCellValue("bad");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }

    @Data
    public static class SimpleImportRow {
        @Excel(name = "名称", orderNum = "0")
        private String name;
    }
}
