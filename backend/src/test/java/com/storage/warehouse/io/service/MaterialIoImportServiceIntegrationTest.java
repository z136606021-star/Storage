package com.storage.warehouse.io.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.system.auth.service.AuthService;
import com.storage.system.user.entity.SysUser;
import com.storage.warehouse.io.excel.MaterialIoExcelColumn;
import com.storage.warehouse.io.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class MaterialIoImportServiceIntegrationTest {

    @Autowired
    private MaterialIoImportService materialIoImportService;

    @Autowired
    private MaterialLedgerMapper materialLedgerMapper;

    @Autowired
    private MaterialIoRecordMapper materialIoRecordMapper;

    @MockBean
    private AuthService authService;

    private MaterialLedger ledger;

    @BeforeEach
    void setUp() {
        materialIoRecordMapper.delete(null);
        materialLedgerMapper.delete(null);

        SysUser operator = new SysUser();
        operator.setId(1L);
        operator.setUsername("tester");
        operator.setDisplayName("测试员");
        when(authService.currentUser()).thenReturn(operator);

        ledger = new MaterialLedger();
        ledger.setCategory("耗材");
        ledger.setGenericName("测试物料");
        ledger.setBrand("品牌A");
        ledger.setName("测试品");
        ledger.setModel("T-001");
        ledger.setBinLocation("1-1-1");
        ledger.setStockQuantity(10);
        materialLedgerMapper.insert(ledger);
    }

    @Test
    void importExcel_validRow_success() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "5", "", "", "入库"));
        MockMultipartFile file = createIoExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        assertThat(currentStock()).isEqualTo(15);
    }

    @Test
    void importExcel_rowError_noDbWrite() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "5", "", "", "入库"));
        MockMultipartFile file = createIoExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isGreaterThan(0);
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(currentStock()).isEqualTo(10);
    }

    @Test
    void importExcel_duplicateMaterialInFile_rejects() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "2", "", "", "入库"));
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "3", "", "", "入库"));
        MockMultipartFile file = createIoExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isGreaterThan(0);
        assertThat(result.getErrors()).anyMatch(error -> error.getMessage().contains("重复物料"));
        assertThat(currentStock()).isEqualTo(10);
    }

    @Test
    void importExcel_outboundExceedsStock_returnsRowErrorWithoutDbWrite() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "11", "员工领用", "", "出库"));
        MockMultipartFile file = createIoExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getErrors()).anyMatch(error -> error.getMessage().contains("出库数量不能超过当前库存"));
        assertThat(materialIoRecordMapper.selectCount(null)).isZero();
        assertThat(currentStock()).isEqualTo(10);
    }

    private int currentStock() {
        return materialLedgerMapper.selectById(ledger.getId()).getStockQuantity();
    }

    private String[] ioRow(
            String category,
            String genericName,
            String brand,
            String name,
            String model,
            String binLocation,
            String quantity,
            String purpose,
            String remark,
            String ioType
    ) {
        return new String[] {
                "1", category, genericName, brand, name, model, binLocation, quantity, purpose, remark, ioType
        };
    }

    private MockMultipartFile createIoExcel(List<String[]> dataRows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("sheet1");
            Row header = sheet.createRow(0);
            String[] headers = MaterialIoExcelColumn.headers();
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
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "material-io.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }
}
