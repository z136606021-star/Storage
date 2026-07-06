package com.storage.system.customer.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.system.customer.excel.SysCustomerExcelColumn;
import com.storage.system.customer.mapper.SysCustomerMapper;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SysCustomerImportServiceIntegrationTest {

    @Autowired
    private SysCustomerImportService sysCustomerImportService;

    @Autowired
    private SysCustomerMapper sysCustomerMapper;

    @BeforeEach
    void setUp() {
        sysCustomerMapper.delete(null);
    }

    @Test
    void importExcel_validRow_success() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(customerRow("CUST-IMP-1", "导入客户", "张三", "13800000000", "imp@example.com"));
        MockMultipartFile file = createCustomerExcel(rows);

        ImportResultVO result = sysCustomerImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        assertThat(sysCustomerMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void importExcel_missingName_returnsRowError() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(customerRow("CUST-IMP-2", "", "李四", "", ""));
        MockMultipartFile file = createCustomerExcel(rows);

        ImportResultVO result = sysCustomerImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(sysCustomerMapper.selectCount(null)).isZero();
    }

    private String[] customerRow(
            String customerCode,
            String name,
            String contactName,
            String phone,
            String email
    ) {
        return new String[] {
                "1", customerCode, name, contactName, phone, email, "", "启用", ""
        };
    }

    private MockMultipartFile createCustomerExcel(List<String[]> dataRows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("sheet1");
            Row header = sheet.createRow(0);
            String[] headers = SysCustomerExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                String[] data = dataRows.get(rowIndex);
                for (int col = 0; col < data.length; col++) {
                    row.createCell(col).setCellValue(data[col]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "customers.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        }
    }
}
