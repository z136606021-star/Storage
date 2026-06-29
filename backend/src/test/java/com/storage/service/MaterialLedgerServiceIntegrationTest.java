package com.storage.service;

import com.storage.dto.MaterialQueryDTO;
import com.storage.dto.MaterialSaveDTO;
import com.storage.entity.WarehouseBin;
import com.storage.entity.WarehouseBom;
import com.storage.mapper.MaterialLedgerMapper;
import com.storage.mapper.WarehouseBinMapper;
import com.storage.mapper.WarehouseBomMapper;
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

        WarehouseBom bom = new WarehouseBom();
        bom.setCategory("耗材");
        bom.setGenericName("测试统称");
        bom.setBrand("品牌A");
        bom.setName("测试品");
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
}
