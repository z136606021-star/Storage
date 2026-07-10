package com.storage.warehouse.service;

import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import com.storage.warehouse.dto.MaterialQueryDTO;
import com.storage.warehouse.dto.MaterialSaveDTO;
import com.storage.warehouse.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.storage.warehouse.query.MaterialLedgerQueryBuilder.STOCK_STATUS_IN_STOCK;
import static com.storage.warehouse.query.MaterialLedgerQueryBuilder.STOCK_STATUS_ZERO_STOCK;

@SpringBootTest
@ActiveProfiles("test")
class MaterialLedgerServiceIntegrationTest {

    @Autowired
    private MaterialLedgerService materialLedgerService;

    @Autowired
    private WarehouseBinMapper warehouseBinMapper;

    @Autowired
    private WarehouseBomMapper warehouseBomMapper;

    @Autowired
    private MaterialIoRecordMapper materialIoRecordMapper;

    @Autowired
    private MaterialLedgerMapper materialLedgerMapper;

    @BeforeEach
    void setUp() {
        materialIoRecordMapper.delete(null);
        materialLedgerMapper.delete(null);
        warehouseBomMapper.delete(null);
        warehouseBinMapper.delete(null);

        WarehouseBin bin = new WarehouseBin();
        bin.setBinCode("1-1-1");
        bin.setRowNo(1);
        bin.setColNo(1);
        bin.setLevelNo(1);
        warehouseBinMapper.insert(bin);

        insertBom("耗材", "测试品");
        insertBom("耗材", "测试品A");
        insertBom("耗材", "零库存测试品");
        insertBom("耗材", "有库存测试品");
        insertBom("电子", "测试品B");
    }

    private void insertBom(String category, String name) {
        WarehouseBom bom = new WarehouseBom();
        bom.setCategory(category);
        bom.setGenericName("测试统称");
        bom.setBrand("品牌A");
        bom.setName(name);
        bom.setModel("M-001");
        warehouseBomMapper.insert(bom);
    }

    @Test
    void createAndPageMaterialLedger() {
        MaterialSaveDTO dto = new MaterialSaveDTO();
        dto.setCategory("耗材");
        dto.setGenericName("测试统称");
        dto.setBrand("品牌A");
        dto.setName("测试品");
        dto.setModel("M-001");
        dto.setBinLocation("1-1-1");

        var created = materialLedgerService.create(dto);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStockQuantity()).isZero();

        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setName("测试");
        query.setPage(1);
        query.setPageSize(10);

        var page = materialLedgerService.page(query);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getModel()).isEqualTo("M-001");
    }

    @Test
    void page_filtersByCategoryAndBinLocation() {
        createLedger("测试品", "耗材", "1-1-1");
        createLedger("测试品B", "电子", "1-1-1");

        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setCategory("耗材");
        query.setBinLocation("1-1-1");
        query.setPage(1);
        query.setPageSize(10);

        var page = materialLedgerService.page(query);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getName()).isEqualTo("测试品");
    }

    @Test
    void page_filtersByInStockStatus() {
        createLedger("零库存测试品", "耗材", "1-1-1");
        var inStock = createLedger("有库存测试品", "耗材", "1-1-1");
        inStock.setStockQuantity(10);
        materialLedgerMapper.updateById(inStock);

        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setStockStatus(STOCK_STATUS_IN_STOCK);
        query.setPage(1);
        query.setPageSize(10);

        var page = materialLedgerService.page(query);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getId()).isEqualTo(inStock.getId());
        assertThat(page.getRecords().get(0).getStockQuantity()).isEqualTo(10);
        assertThat(page.getRecords().get(0).getName()).isEqualTo("有库存测试品");
    }

    @Test
    void page_filtersByZeroStockStatus() {
        var zeroStock = createLedger("零库存测试品", "耗材", "1-1-1");
        var inStock = createLedger("有库存测试品", "耗材", "1-1-1");
        inStock.setStockQuantity(10);
        materialLedgerMapper.updateById(inStock);

        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setStockStatus(STOCK_STATUS_ZERO_STOCK);
        query.setPage(1);
        query.setPageSize(10);

        var page = materialLedgerService.page(query);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getId()).isEqualTo(zeroStock.getId());
        assertThat(page.getRecords().get(0).getStockQuantity()).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void page_withoutStockFilter_returnsAllRecords(String stockStatus) {
        createLedger("零库存测试品", "耗材", "1-1-1");
        var inStock = createLedger("有库存测试品", "耗材", "1-1-1");
        inStock.setStockQuantity(10);
        materialLedgerMapper.updateById(inStock);

        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setStockStatus(stockStatus);
        query.setPage(1);
        query.setPageSize(10);

        var page = materialLedgerService.page(query);

        assertThat(page.getRecords()).hasSize(2);
    }

    @Test
    void listByQuery_appliesStockStatusForExportPath() {
        createLedger("零库存测试品", "耗材", "1-1-1");
        var inStock = createLedger("有库存测试品", "耗材", "1-1-1");
        inStock.setStockQuantity(10);
        materialLedgerMapper.updateById(inStock);

        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setStockStatus(STOCK_STATUS_IN_STOCK);

        var records = materialLedgerService.listByQuery(query);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getName()).isEqualTo("有库存测试品");
    }

    @Test
    void page_rejectsInvalidStockStatus() {
        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setStockStatus("INVALID");
        query.setPage(1);
        query.setPageSize(10);

        assertThatThrownBy(() -> materialLedgerService.page(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("库存状态无效");
    }

    @Test
    void exportByIds_returnsOnlySelectedRecords() throws Exception {
        var first = createLedger("测试品", "耗材", "1-1-1");
        createLedger("测试品A", "耗材", "1-1-1");

        MaterialQueryDTO query = new MaterialQueryDTO();
        query.setIds(java.util.List.of(first.getId()));

        var records = materialLedgerService.listByQuery(query);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getName()).isEqualTo("测试品");
    }

    private com.storage.warehouse.entity.MaterialLedger createLedger(String name, String category, String binLocation) {
        MaterialSaveDTO dto = new MaterialSaveDTO();
        dto.setCategory(category);
        dto.setGenericName("测试统称");
        dto.setBrand("品牌A");
        dto.setName(name);
        dto.setModel("M-001");
        dto.setBinLocation(binLocation);
        return materialLedgerService.create(dto);
    }
}
