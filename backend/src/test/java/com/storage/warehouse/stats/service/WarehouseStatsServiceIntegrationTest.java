package com.storage.warehouse.stats.service;

import com.storage.warehouse.io.dto.MaterialIoBatchItemDTO;
import com.storage.warehouse.io.dto.MaterialIoBatchSaveDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.system.user.entity.SysUser;
import com.storage.common.exception.BusinessException;
import com.storage.warehouse.io.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.bin.entity.WarehouseBin;
import com.storage.warehouse.bin.mapper.WarehouseBinMapper;
import com.storage.warehouse.bom.entity.WarehouseBom;
import com.storage.warehouse.bom.mapper.WarehouseBomMapper;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import com.storage.system.auth.service.AuthService;
import com.storage.warehouse.io.service.MaterialIoService;
import com.storage.warehouse.safety.entity.SafetyStock;
import com.storage.warehouse.safety.mapper.SafetyStockMapper;
import com.storage.warehouse.stats.dto.WarehouseStatsOverviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class WarehouseStatsServiceIntegrationTest {

    @Autowired
    private WarehouseStatsService warehouseStatsService;

    @Autowired
    private MaterialIoService materialIoService;

    @Autowired
    private MaterialLedgerMapper materialLedgerMapper;

    @Autowired
    private MaterialIoRecordMapper materialIoRecordMapper;

    @Autowired
    private SafetyStockMapper safetyStockMapper;

    @Autowired
    private WarehouseBomMapper warehouseBomMapper;

    @Autowired
    private WarehouseBinMapper warehouseBinMapper;

    @MockBean
    private AuthService authService;

    private Long ledgerId;
    private Long bomId;

    @BeforeEach
    void setUp() {
        materialIoRecordMapper.delete(null);
        safetyStockMapper.delete(null);
        materialLedgerMapper.delete(null);
        warehouseBinMapper.delete(null);
        warehouseBomMapper.delete(null);

        SysUser operator = new SysUser();
        operator.setId(1L);
        operator.setUsername("tester");
        operator.setDisplayName("???");
        when(authService.currentUser()).thenReturn(operator);

        WarehouseBom bom = new WarehouseBom();
        bom.setCategory("??");
        bom.setGenericName("????");
        bom.setBrand("??A");
        bom.setName("???");
        bom.setModel("S-001");
        warehouseBomMapper.insert(bom);
        bomId = bom.getId();

        WarehouseBin bin = new WarehouseBin();
        bin.setBinCode("1-1-1");
        bin.setRowNo(1);
        bin.setColNo(1);
        bin.setLevelNo(1);
        warehouseBinMapper.insert(bin);

        MaterialLedger ledger = new MaterialLedger();
        ledger.setCategory("??");
        ledger.setGenericName("????");
        ledger.setBrand("??A");
        ledger.setName("???");
        ledger.setModel("S-001");
        ledger.setBinLocation("1-1-1");
        ledger.setStockQuantity(5);
        materialLedgerMapper.insert(ledger);
        ledgerId = ledger.getId();
    }

    @Test
    void overview_countsLedgerStockWarningsAndRecentIo() {
        SafetyStock safetyStock = new SafetyStock();
        safetyStock.setMaterialLedgerId(ledgerId);
        safetyStock.setSafetyQuantity(10);
        safetyStock.setWarningEnabled(true);
        safetyStockMapper.insert(safetyStock);

        MaterialIoBatchSaveDTO inbound = new MaterialIoBatchSaveDTO();
        inbound.setIoType("IN");
        inbound.setItems(List.of(inboundItem(bomId, "1-1-1", 2)));
        materialIoService.batchCreate(inbound);

        MaterialIoBatchSaveDTO outbound = new MaterialIoBatchSaveDTO();
        outbound.setIoType("OUT");
        outbound.setItems(List.of(batchItem(ledgerId, 1, "EMPLOYEE_PICKUP")));
        materialIoService.batchCreate(outbound);

        WarehouseStatsOverviewVO overview = warehouseStatsService.overview(7);

        assertThat(overview.getTotalLedgerCount()).isEqualTo(1);
        assertThat(overview.getTotalStockQuantity()).isEqualTo(6);
        assertThat(overview.getWarningMaterialCount()).isEqualTo(1);
        assertThat(overview.getInboundRecordCount()).isEqualTo(1);
        assertThat(overview.getOutboundRecordCount()).isEqualTo(1);
        assertThat(overview.getInboundQuantitySum()).isEqualTo(2);
        assertThat(overview.getOutboundQuantitySum()).isEqualTo(1);
        assertThat(overview.getWarningMaterials()).hasSize(1);
    }

    @Test
    void batchOutbound_projectUseWithoutRef_rejects() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("OUT");
        MaterialIoBatchItemDTO item = batchItem(ledgerId, 1, "PROJECT_USE");
        batch.setItems(List.of(item));

        assertThatThrownBy(() -> materialIoService.batchCreate(batch))
                .isInstanceOf(BusinessException.class);
    }

    private MaterialIoBatchItemDTO batchItem(Long materialLedgerId, int quantity) {
        MaterialIoBatchItemDTO item = new MaterialIoBatchItemDTO();
        item.setMaterialLedgerId(materialLedgerId);
        item.setQuantity(quantity);
        return item;
    }

    private MaterialIoBatchItemDTO batchItem(Long materialLedgerId, int quantity, String purpose) {
        MaterialIoBatchItemDTO item = batchItem(materialLedgerId, quantity);
        item.setPurpose(purpose);
        return item;
    }

    private MaterialIoBatchItemDTO inboundItem(Long bomId, String binLocation, int quantity) {
        MaterialIoBatchItemDTO item = new MaterialIoBatchItemDTO();
        item.setBomId(bomId);
        item.setBinLocation(binLocation);
        item.setQuantity(quantity);
        return item;
    }
}
