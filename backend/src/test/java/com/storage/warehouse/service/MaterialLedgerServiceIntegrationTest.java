package com.storage.warehouse.service;

import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import com.storage.warehouse.dto.MaterialQueryDTO;
import com.storage.warehouse.dto.MaterialSaveDTO;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

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
    private MaterialLedgerMapper materialLedgerMapper;

    @BeforeEach
    void setUp() {
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
