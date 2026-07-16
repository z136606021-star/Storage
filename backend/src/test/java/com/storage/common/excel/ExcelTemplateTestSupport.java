package com.storage.common.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class ExcelTemplateTestSupport {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExcelTemplateTestSupport() {
    }

    public static MockMultipartFile fillTemplate(
            byte[] template,
            String filename,
            Map<Integer, ?> values
    ) throws IOException {
        return fillTemplateRows(template, filename, List.of(values));
    }

    public static MockMultipartFile fillTemplateRows(
            byte[] template,
            String filename,
            List<? extends Map<Integer, ?>> rows
    ) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(template));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (Map.Entry<Integer, ?> entry : rows.get(rowIndex).entrySet()) {
                    Object value = entry.getValue();
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Number number) {
                        row.createCell(entry.getKey()).setCellValue(number.doubleValue());
                    } else {
                        row.createCell(entry.getKey()).setCellValue(String.valueOf(value));
                    }
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", filename, XLSX_CONTENT_TYPE, out.toByteArray());
        }
    }
}
