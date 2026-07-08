package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.system.auth.service.AuthService;
import com.storage.system.user.entity.SysUser;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import com.storage.warehouse.entity.MaterialIoRecord;
import com.storage.warehouse.excel.MaterialIoImportTemplateColumn;
import com.storage.warehouse.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class MaterialIoImportServiceIntegrationTest {

    private static final DateTimeFormatter OPERATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MaterialIoImportService materialIoImportService;

    @Autowired
    private MaterialLedgerMapper materialLedgerMapper;

    @Autowired
    private MaterialIoRecordMapper materialIoRecordMapper;

    @Autowired
    private WarehouseBomMapper warehouseBomMapper;

    @Autowired
    private WarehouseBinMapper warehouseBinMapper;

    @MockBean
    private AuthService authService;

    private MaterialLedger ledger;

    @BeforeEach
    void setUp() {
        materialIoRecordMapper.delete(null);
        materialLedgerMapper.delete(null);
        warehouseBomMapper.delete(null);
        warehouseBinMapper.delete(null);

        SysUser operator = new SysUser();
        operator.setId(1L);
        operator.setUsername("tester");
        operator.setDisplayName("测试员");
        when(authService.currentUser()).thenReturn(operator);

        WarehouseBom bom = new WarehouseBom();
        bom.setCategory("耗材");
        bom.setGenericName("测试物料");
        bom.setBrand("品牌A");
        bom.setName("测试品");
        bom.setModel("T-001");
        warehouseBomMapper.insert(bom);

        WarehouseBin bin = new WarehouseBin();
        bin.setBinCode("1-1-1");
        bin.setRowNo(1);
        bin.setColNo(1);
        bin.setLevelNo(1);
        warehouseBinMapper.insert(bin);

        ledger = new MaterialLedger();
        ledger.setCategory("耗材");
        ledger.setGenericName("测试物料");
        ledger.setBrand("品牌A");
        ledger.setName("测试品");
        ledger.setModel("T-001");
        ledger.setBinLocation("1-1-1");
        ledger.setStockQuantity(10);
        LocalDateTime seededAt = LocalDateTime.now().minusMinutes(10).withNano(0);
        ledger.setCreatedAt(seededAt);
        ledger.setUpdatedAt(seededAt);
        materialLedgerMapper.insert(ledger);
    }

    @Test
    void importExcel_validRow_success() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "5", "", "", "入库"));
        MockMultipartFile file = createImportTemplateExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        assertThat(currentStock()).isEqualTo(15);
    }

    @Test
    void importExcel_inboundFromBomAndBin_createsLedgerWhenMissing() throws IOException {
        materialIoRecordMapper.delete(null);
        materialLedgerMapper.delete(null);

        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "5", "", "", "入库"));
        MockMultipartFile file = createImportTemplateExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        List<MaterialLedger> ledgers = materialLedgerMapper.selectList(null);
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).getStockQuantity()).isEqualTo(5);
        assertThat(materialIoRecordMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void importExcel_inboundFromBomAndBin_withPastOperatedAt_createsLedgerAndRecord() throws IOException {
        materialIoRecordMapper.delete(null);
        materialLedgerMapper.delete(null);
        LocalDateTime operatedAt = LocalDateTime.now().minusMinutes(1).withNano(0);

        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow(
                "耗材",
                "测试物料",
                "品牌A",
                "测试品",
                "T-001",
                "1-1-1",
                "5",
                "",
                "",
                "入库",
                OPERATED_AT_FORMATTER.format(operatedAt)
        ));
        MockMultipartFile file = createImportTemplateExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        assertThat(materialLedgerMapper.selectList(null)).singleElement()
                .extracting(MaterialLedger::getStockQuantity)
                .isEqualTo(5);
        assertThat(materialIoRecordMapper.selectList(null)).singleElement()
                .extracting(MaterialIoRecord::getOperatedAt)
                .isEqualTo(operatedAt);
    }

    @Test
    void importExcel_inboundWithoutConfiguredBom_returnsRowErrorWithoutDbWrite() throws IOException {
        materialIoRecordMapper.delete(null);
        materialLedgerMapper.delete(null);
        warehouseBomMapper.delete(null);

        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "5", "", "", "入库"));
        MockMultipartFile file = createImportTemplateExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isGreaterThan(0);
        assertThat(result.getErrors()).anyMatch(error -> error.getMessage().contains("物料清单"));
        assertThat(materialLedgerMapper.selectCount(null)).isZero();
        assertThat(materialIoRecordMapper.selectCount(null)).isZero();
    }

    @Test
    void importExcel_importTemplateLayout_withOperatedAt_persistsTimestamp() throws IOException {
        LocalDateTime operatedAt = LocalDateTime.now().minusMinutes(1).withNano(0);
        String operatedAtText = OPERATED_AT_FORMATTER.format(operatedAt);

        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("耗材", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "5", "", "", "入库", operatedAtText));
        MockMultipartFile file = createImportTemplateExcel(rows);

        ImportResultVO result = materialIoImportService.importExcel(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
        assertThat(currentStock()).isEqualTo(15);

        MaterialIoRecord record = materialIoRecordMapper.selectList(null).get(0);
        assertThat(record.getOperatedAt()).isEqualTo(operatedAt);
    }

    @Test
    void importExcel_rowError_noDbWrite() throws IOException {
        List<String[]> rows = new ArrayList<>();
        rows.add(ioRow("", "测试物料", "品牌A", "测试品", "T-001", "1-1-1", "5", "", "", "入库"));
        MockMultipartFile file = createImportTemplateExcel(rows);

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
        MockMultipartFile file = createImportTemplateExcel(rows);

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
        MockMultipartFile file = createImportTemplateExcel(rows);

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
        return ioRow(category, genericName, brand, name, model, binLocation, quantity, purpose, remark, ioType, "");
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
            String ioType,
            String operatedAt
    ) {
        return new String[] {
                "1", category, genericName, brand, name, model, binLocation, quantity, purpose, remark, ioType, operatedAt
        };
    }

    private MockMultipartFile createImportTemplateExcel(List<String[]> dataRows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("sheet1");
            Row header = sheet.createRow(0);
            String[] headers = MaterialIoImportTemplateColumn.headers();
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
