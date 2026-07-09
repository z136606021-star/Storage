package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.excel.WarehouseBinImportTemplateColumn;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WarehouseBinImportServiceIntegrationTest {

    @Autowired
    private WarehouseBinImportService warehouseBinImportService;

    @Autowired
    private WarehouseBinService warehouseBinService;

    @Autowired
    private WarehouseBinMapper warehouseBinMapper;

    @BeforeEach
    void setUp() {
        warehouseBinMapper.delete(null);
    }

    @Test
    void import_createsRowOnlyAndRowColBins() throws IOException {
        MockMultipartFile file = buildImportFile(
                new String[] { "排", "列", "层", "备注" },
                new Object[] { 3, null, null, "仅排" },
                new Object[] { 4, 5, null, "排+列" }
        );

        ImportResultVO result = warehouseBinImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailCount()).isZero();
        assertThat(warehouseBinService.listAllCodes()).containsExactlyInAnyOrder("3", "4-5");
    }

    @Test
    void import_duplicateRowOnlyReportsFailure() throws IOException {
        warehouseBinService.create(binDto(7, null, null));

        MockMultipartFile file = buildImportFile(
                new String[] { "排", "列", "层", "备注" },
                new Object[] { 7, null, null, "重复" }
        );

        ImportResultVO result = warehouseBinImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("Bin位编号已存在");
    }

    @Test
    void exportTemplate_writesImportHeadersWithoutBinCode() throws IOException {
        byte[] bytes = warehouseBinService.exportTemplate();

        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            String[] headers = WarehouseBinImportTemplateColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }
            assertThat(sheet.getRow(1)).isNull();
        }
    }

    private com.storage.warehouse.dto.WarehouseBinSaveDTO binDto(Integer row, Integer col, Integer level) {
        com.storage.warehouse.dto.WarehouseBinSaveDTO dto = new com.storage.warehouse.dto.WarehouseBinSaveDTO();
        dto.setRowNo(row);
        dto.setColNo(col);
        dto.setLevelNo(level);
        return dto;
    }

    private MockMultipartFile buildImportFile(String[] headers, Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bin位");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                Object[] values = rows[rowIndex];
                for (int colIndex = 0; colIndex < values.length; colIndex++) {
                    Object value = values[colIndex];
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Number number) {
                        row.createCell(colIndex).setCellValue(number.doubleValue());
                    } else {
                        row.createCell(colIndex).setCellValue(String.valueOf(value));
                    }
                }
            }
            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "bins.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        }
    }
}
