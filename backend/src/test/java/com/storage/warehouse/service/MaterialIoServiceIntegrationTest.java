package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.auth.service.AuthService;
import com.storage.system.user.entity.SysUser;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import com.storage.warehouse.dto.MaterialIoBatchItemDTO;
import com.storage.warehouse.dto.MaterialIoBatchSaveDTO;
import com.storage.warehouse.dto.MaterialIoQueryDTO;
import com.storage.warehouse.dto.MaterialIoRecordVO;
import com.storage.warehouse.dto.MaterialIoSaveDTO;
import com.storage.warehouse.dto.MaterialIoUpdateDTO;
import com.storage.warehouse.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.entity.SafetyStock;
import com.storage.warehouse.mapper.SafetyStockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class MaterialIoServiceIntegrationTest {

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
        operator.setDisplayName("测试员");
        when(authService.currentUser()).thenReturn(operator);

        WarehouseBom bom = new WarehouseBom();
        bom.setCategory("耗材");
        bom.setGenericName("测试物料");
        bom.setBrand("品牌A");
        bom.setName("测试品");
        bom.setModel("T-001");
        warehouseBomMapper.insert(bom);
        bomId = bom.getId();

        insertBin("1-1-1", 1, 1, 1);
        insertBin("2-3-4", 2, 3, 4);

        MaterialLedger ledger = new MaterialLedger();
        ledger.setCategory("耗材");
        ledger.setGenericName("测试物料");
        ledger.setBrand("品牌A");
        ledger.setName("测试品");
        ledger.setModel("T-001");
        ledger.setBinLocation("1-1-1");
        ledger.setStockQuantity(10);
        materialLedgerMapper.insert(ledger);
        ledgerId = ledger.getId();
    }

    @Test
    void batchOutbound_withoutPurpose_rejects() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("OUT");
        batch.setItems(List.of(batchItem(ledgerId, 3)));

        assertThatThrownBy(() -> materialIoService.batchCreate(batch))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用途");
    }

    @Test
    void batchCreate_withOperatedAt_persistsTimestamp() {
        MaterialLedger ledger = materialLedgerMapper.selectById(ledgerId);
        LocalDateTime operatedAt = ledger.getCreatedAt();

        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("IN");
        batch.setOperatedAt(operatedAt);
        batch.setItems(List.of(inboundItem(bomId, "1-1-1", 2)));

        var created = materialIoService.batchCreate(batch).get(0);

        assertThat(created.getOperatedAt()).isEqualTo(operatedAt);
    }

    @Test
    void safetyHints_returnsConfiguredThreshold() {
        SafetyStock safetyStock = new SafetyStock();
        safetyStock.setMaterialLedgerId(ledgerId);
        safetyStock.setSafetyQuantity(12);
        safetyStock.setWarningEnabled(true);
        safetyStockMapper.insert(safetyStock);

        var hints = materialIoService.safetyHints(List.of(ledgerId));

        assertThat(hints).hasSize(1);
        assertThat(hints.get(0).getSafetyQuantity()).isEqualTo(12);
        assertThat(hints.get(0).getWarningEnabled()).isTrue();
        assertThat(hints.get(0).getCurrentStock()).isEqualTo(10);
    }

    @Test
    void page_filtersByPurpose() {
        materialIoService.batchCreate(outboundBatch(ledgerId, 2, "EMPLOYEE_PICKUP"));

        MaterialIoQueryDTO query = new MaterialIoQueryDTO();
        query.setPurpose("EMPLOYEE_PICKUP");
        query.setPage(1);
        query.setPageSize(20);

        var page = materialIoService.page(query);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getPurpose()).isEqualTo("EMPLOYEE_PICKUP");
    }

    @Test
    void batchOutbound_sameLedgerTwice_rejectsDuplicate() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("OUT");
        batch.setItems(List.of(
                batchItem(ledgerId, 3, "EMPLOYEE_PICKUP"),
                batchItem(ledgerId, 8, "EMPLOYEE_PICKUP")
        ));

        assertThatThrownBy(() -> materialIoService.batchCreate(batch))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重复物料");

        assertThat(currentStock()).isEqualTo(10);
    }

    @Test
    void batchOutbound_secondLedgerExceedsStock_rollsBack() {
        MaterialLedger second = new MaterialLedger();
        second.setCategory("耗材");
        second.setGenericName("测试物料2");
        second.setBrand("品牌B");
        second.setName("测试品2");
        second.setModel("T-002");
        second.setBinLocation("1-1-2");
        second.setStockQuantity(5);
        materialLedgerMapper.insert(second);

        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("OUT");
        batch.setItems(List.of(
                batchItem(ledgerId, 3, "EMPLOYEE_PICKUP"),
                batchItem(second.getId(), 8, "EMPLOYEE_PICKUP")
        ));

        assertThatThrownBy(() -> materialIoService.batchCreate(batch))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存");

        assertThat(currentStock()).isEqualTo(10);
        assertThat(materialLedgerMapper.selectById(second.getId()).getStockQuantity()).isEqualTo(5);
    }

    @Test
    void inbound_increasesStock() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("IN");
        batch.setItems(List.of(inboundItem(bomId, "1-1-1", 5)));

        materialIoService.batchCreate(batch);

        assertThat(currentStock()).isEqualTo(15);
    }

    @Test
    void batchInbound_fromBomAndBin_createsLedgerAndIncreasesStock() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("IN");
        batch.setItems(List.of(inboundItem(bomId, "2-3-4", 6)));

        var created = materialIoService.batchCreate(batch).get(0);

        MaterialLedger createdLedger = materialLedgerMapper.selectById(created.getMaterialLedgerId());
        assertThat(createdLedger.getCategory()).isEqualTo("耗材");
        assertThat(createdLedger.getGenericName()).isEqualTo("测试物料");
        assertThat(createdLedger.getBrand()).isEqualTo("品牌A");
        assertThat(createdLedger.getName()).isEqualTo("测试品");
        assertThat(createdLedger.getModel()).isEqualTo("T-001");
        assertThat(createdLedger.getBinLocation()).isEqualTo("2-3-4");
        assertThat(createdLedger.getStockQuantity()).isEqualTo(6);
    }

    @Test
    void batchInbound_sameBomAndBin_increasesExistingLedger() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("IN");
        batch.setItems(List.of(inboundItem(bomId, "1-1-1", 2)));

        materialIoService.batchCreate(batch);

        assertThat(currentStock()).isEqualTo(12);
    }

    @Test
    void batchInbound_unknownBin_rejects() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("IN");
        batch.setItems(List.of(inboundItem(bomId, "9-9-9", 2)));

        assertThatThrownBy(() -> materialIoService.batchCreate(batch))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bin位不存在");
    }

    @Test
    void updateOutboundQuantity_recalculatesStock() {
        var created = materialIoService.batchCreate(outboundBatch(ledgerId, 5)).get(0);

        MaterialIoUpdateDTO update = new MaterialIoUpdateDTO();
        update.setQuantity(3);
        materialIoService.update(created.getId(), update);

        assertThat(currentStock()).isEqualTo(7);
    }

    @Test
    void update_onlyQuantityAndRemark() {
        var created = materialIoService.batchCreate(outboundBatch(ledgerId, 5)).get(0);

        MaterialIoUpdateDTO update = new MaterialIoUpdateDTO();
        update.setQuantity(4);
        update.setRemark("调整备注");
        update.setPurpose("MACHINING");
        MaterialIoRecordVO updated = materialIoService.update(created.getId(), update);

        assertThat(updated.getQuantity()).isEqualTo(4);
        assertThat(updated.getRemark()).isEqualTo("调整备注");
        assertThat(updated.getPurpose()).isEqualTo("MACHINING");
        assertThat(updated.getIoType()).isEqualTo("OUT");
        assertThat(updated.getMaterialLedgerId()).isEqualTo(ledgerId);
        assertThat(currentStock()).isEqualTo(6);
    }

    @Test
    void deleteOutboundRecord_restoresStock() {
        var created = materialIoService.batchCreate(outboundBatch(ledgerId, 5)).get(0);

        materialIoService.delete(created.getId());

        assertThat(currentStock()).isEqualTo(10);
    }

    @Test
    void importBatch_failureRollsBackEntireBatch() {
        List<MaterialIoSaveDTO> dtos = new ArrayList<>();
        dtos.add(saveDto(ledgerId, "OUT", 3, "EMPLOYEE_PICKUP"));
        dtos.add(saveDto(ledgerId, "OUT", 8, "EMPLOYEE_PICKUP"));

        assertThatThrownBy(() -> materialIoService.importBatch(dtos))
                .isInstanceOf(BusinessException.class);

        assertThat(currentStock()).isEqualTo(10);
    }

    @Test
    void importBatch_duplicateLedger_rejects() {
        List<MaterialIoSaveDTO> dtos = List.of(
                saveDto(ledgerId, "IN", 2),
                saveDto(ledgerId, "IN", 3)
        );

        assertThatThrownBy(() -> materialIoService.importBatch(dtos))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重复物料");

        assertThat(currentStock()).isEqualTo(10);
    }

    @Test
    void page_filtersByMaterialLedgerId() {
        MaterialLedger second = new MaterialLedger();
        second.setCategory("耗材");
        second.setGenericName("测试物料2");
        second.setBrand("品牌B");
        second.setName("测试品2");
        second.setModel("T-002");
        second.setBinLocation("1-1-2");
        second.setStockQuantity(10);
        materialLedgerMapper.insert(second);

        materialIoService.batchCreate(outboundBatch(ledgerId, 2));
        materialIoService.batchCreate(outboundBatch(second.getId(), 3));

        MaterialIoQueryDTO query = new MaterialIoQueryDTO();
        query.setMaterialLedgerId(ledgerId);
        query.setPage(1);
        query.setPageSize(20);

        var page = materialIoService.page(query);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getMaterialLedgerId()).isEqualTo(ledgerId);
    }

    @Test
    void batchOutbound_projectUse_persistsProjectRef() {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("OUT");
        MaterialIoBatchItemDTO item = batchItem(ledgerId, 2, "PROJECT_USE");
        item.setProjectRef("PRJ-2026-001");
        batch.setItems(List.of(item));

        var created = materialIoService.batchCreate(batch).get(0);

        assertThat(created.getPurpose()).isEqualTo("PROJECT_USE");
        assertThat(created.getProjectRef()).isEqualTo("PRJ-2026-001");
    }

    private int currentStock() {
        return materialLedgerMapper.selectById(ledgerId).getStockQuantity();
    }

    private void insertBin(String binCode, int rowNo, int colNo, int levelNo) {
        WarehouseBin bin = new WarehouseBin();
        bin.setBinCode(binCode);
        bin.setRowNo(rowNo);
        bin.setColNo(colNo);
        bin.setLevelNo(levelNo);
        warehouseBinMapper.insert(bin);
    }

    private MaterialIoBatchSaveDTO outboundBatch(Long ledgerId, int quantity) {
        return outboundBatch(ledgerId, quantity, "EMPLOYEE_PICKUP");
    }

    private MaterialIoBatchSaveDTO outboundBatch(Long ledgerId, int quantity, String purpose) {
        MaterialIoBatchSaveDTO batch = new MaterialIoBatchSaveDTO();
        batch.setIoType("OUT");
        batch.setItems(List.of(batchItem(ledgerId, quantity, purpose)));
        return batch;
    }

    private MaterialIoBatchItemDTO batchItem(Long ledgerId, int quantity) {
        return batchItem(ledgerId, quantity, null);
    }

    private MaterialIoBatchItemDTO batchItem(Long ledgerId, int quantity, String purpose) {
        MaterialIoBatchItemDTO item = new MaterialIoBatchItemDTO();
        item.setMaterialLedgerId(ledgerId);
        item.setQuantity(quantity);
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

    private MaterialIoSaveDTO saveDto(Long ledgerId, String ioType, int quantity) {
        return saveDto(ledgerId, ioType, quantity, null);
    }

    private MaterialIoSaveDTO saveDto(Long ledgerId, String ioType, int quantity, String purpose) {
        MaterialIoSaveDTO dto = new MaterialIoSaveDTO();
        dto.setMaterialLedgerId(ledgerId);
        dto.setIoType(ioType);
        dto.setQuantity(quantity);
        dto.setPurpose(purpose);
        return dto;
    }
}
