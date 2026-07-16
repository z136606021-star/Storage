package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.excel.WarehouseBomImportTemplateColumn;
import com.storage.warehouse.mapper.WarehouseBomImageMapper;
import com.storage.warehouse.mapper.WarehouseBomMapper;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WarehouseBomImportServiceIntegrationTest {

    @Autowired
    private WarehouseBomImportService warehouseBomImportService;

    @Autowired
    private WarehouseBomService warehouseBomService;

    @Autowired
    private WarehouseBomMapper warehouseBomMapper;

    @Autowired
    private WarehouseBomImageMapper warehouseBomImageMapper;

    @BeforeEach
    void setUp() {
        warehouseBomImageMapper.delete(null);
        warehouseBomMapper.delete(null);
    }

    @Test
    void downloadedTemplate_canBeFilledAndImportedWithoutSpecification() throws IOException {
        byte[] template = warehouseBomService.exportTemplate();
        MockMultipartFile file;
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(template));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            String[] headers = WarehouseBomImportTemplateColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(header, i)).isEqualTo(headers[i]);
            }

            Row row = sheet.createRow(1);
            row.createCell(WarehouseBomImportTemplateColumn.CATEGORY.getIndex()).setCellValue("耗材");
            row.createCell(WarehouseBomImportTemplateColumn.GENERIC_NAME.getIndex()).setCellValue("密封圈");
            row.createCell(WarehouseBomImportTemplateColumn.BRAND.getIndex()).setCellValue("三环");
            row.createCell(WarehouseBomImportTemplateColumn.NAME.getIndex()).setCellValue("O型密封圈");
            row.createCell(WarehouseBomImportTemplateColumn.REMARK.getIndex()).setCellValue("批量导入");
            workbook.write(out);
            file = new MockMultipartFile(
                    "file",
                    "warehouse-bom.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        }

        ImportResultVO result = warehouseBomImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        assertThat(warehouseBomMapper.selectList(null)).singleElement().satisfies(record -> {
            assertThat(record.getName()).isEqualTo("O型密封圈");
            assertThat(record.getRemark()).isEqualTo("批量导入");
        });
    }
}
