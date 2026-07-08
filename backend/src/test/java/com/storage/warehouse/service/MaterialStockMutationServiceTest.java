package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.exception.BusinessException;
import com.storage.warehouse.dto.MaterialIoSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialStockMutationServiceTest {

    @Mock
    private MaterialLedgerMapper materialLedgerMapper;

    @InjectMocks
    private MaterialStockMutationServiceImpl materialStockMutationService;

    private MaterialLedger ledger;

    @BeforeEach
    void setUp() {
        ledger = new MaterialLedger();
        ledger.setId(1L);
        ledger.setName("测试品");
        ledger.setModel("T-001");
        ledger.setStockQuantity(10);
    }

    @Test
    void applyEffect_inbound_increasesStock() {
        materialStockMutationService.applyEffect(ledger, "IN", 3, false);
        assertThat(ledger.getStockQuantity()).isEqualTo(13);
    }

    @Test
    void applyEffect_outbound_decreasesStock() {
        materialStockMutationService.applyEffect(ledger, "OUT", 4, false);
        assertThat(ledger.getStockQuantity()).isEqualTo(6);
    }

    @Test
    void applyEffect_reverse_flipsDelta() {
        materialStockMutationService.applyEffect(ledger, "OUT", 4, true);
        assertThat(ledger.getStockQuantity()).isEqualTo(14);
    }

    @Test
    void applyEffect_rejectsNegativeStock() {
        assertThatThrownBy(() -> materialStockMutationService.applyEffect(ledger, "OUT", 11, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void validateOutbound_rejectsWhenQuantityExceedsStock() {
        assertThatThrownBy(() -> materialStockMutationService.validateOutbound(ledger, "OUT", 11))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("出库数量不能超过当前库存");
    }

    @Test
    void validateOutbound_ignoresInbound() {
        materialStockMutationService.validateOutbound(ledger, "IN", 100);
        assertThat(ledger.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void lockLedgersInOrder_sortsIdsBeforeLocking() {
        when(materialLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        MaterialLedger ledger2 = new MaterialLedger();
        ledger2.setId(2L);
        ledger2.setStockQuantity(5);
        when(materialLedgerMapper.selectByIdForUpdate(2L)).thenReturn(ledger2);

        Map<Long, MaterialLedger> locked = materialStockMutationService.lockLedgersInOrder(List.of(2L, 1L, 2L));

        assertThat(locked).containsOnlyKeys(1L, 2L);
    }

    @Test
    void collectImportStockErrors_reportsOutboundExceedingStock() {
        MaterialIoSaveDTO dto = new MaterialIoSaveDTO();
        dto.setMaterialLedgerId(1L);
        dto.setIoType("OUT");
        dto.setQuantity(11);

        List<ImportResultVO.ImportErrorVO> errors = materialStockMutationService.collectImportStockErrors(
                List.of(new MaterialStockMutationService.ImportStockSimulationRow(2, dto)),
                Map.of(1L, ledger)
        );

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getRow()).isEqualTo(2);
        assertThat(errors.get(0).getMessage()).contains("出库数量不能超过当前库存");
    }

    @Test
    void collectImportStockErrors_accumulatesOutboundRowsForSameLedger() {
        MaterialIoSaveDTO first = outboundDto(6);
        MaterialIoSaveDTO second = outboundDto(5);

        List<ImportResultVO.ImportErrorVO> errors = materialStockMutationService.collectImportStockErrors(
                List.of(
                        new MaterialStockMutationService.ImportStockSimulationRow(2, first),
                        new MaterialStockMutationService.ImportStockSimulationRow(3, second)
                ),
                Map.of(1L, ledger)
        );

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getRow()).isEqualTo(3);
    }

    @Test
    void collectImportStockErrors_allowsInboundThenOutboundWithinStock() {
        MaterialIoSaveDTO inbound = new MaterialIoSaveDTO();
        inbound.setMaterialLedgerId(1L);
        inbound.setIoType("IN");
        inbound.setQuantity(5);

        MaterialIoSaveDTO outbound = outboundDto(15);

        List<ImportResultVO.ImportErrorVO> errors = materialStockMutationService.collectImportStockErrors(
                List.of(
                        new MaterialStockMutationService.ImportStockSimulationRow(2, inbound),
                        new MaterialStockMutationService.ImportStockSimulationRow(3, outbound)
                ),
                Map.of(1L, ledger)
        );

        assertThat(errors).isEmpty();
    }

    private MaterialIoSaveDTO outboundDto(int quantity) {
        MaterialIoSaveDTO dto = new MaterialIoSaveDTO();
        dto.setMaterialLedgerId(1L);
        dto.setIoType("OUT");
        dto.setQuantity(quantity);
        return dto;
    }
}
