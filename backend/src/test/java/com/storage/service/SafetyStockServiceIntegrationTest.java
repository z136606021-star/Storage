package com.storage.service;

import com.storage.dto.SafetyStockQueryDTO;
import com.storage.dto.SafetyStockRecordVO;
import com.storage.dto.SafetyStockUpdateDTO;
import com.storage.entity.MaterialLedger;
import com.storage.mapper.MaterialLedgerMapper;
import com.storage.mapper.SafetyStockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SafetyStockServiceIntegrationTest {

    @Autowired
    private SafetyStockService safetyStockService;

    @Autowired
    private MaterialLedgerMapper materialLedgerMapper;

    @Autowired
    private SafetyStockMapper safetyStockMapper;

    private Long lowStockLedgerId;
    private Long sufficientStockLedgerId;

    @BeforeEach
    void setUp() {
        safetyStockMapper.delete(null);
        materialLedgerMapper.delete(null);

        lowStockLedgerId = insertLedger("低库存品", 5);
        sufficientStockLedgerId = insertLedger("充足品", 50);
    }

    @Test
    void page_unconfiguredDefaultsToZeroAndNoWarning() {
        var result = safetyStockService.page(new SafetyStockQueryDTO());

        SafetyStockRecordVO unconfigured = findByLedgerId(result.getRecords(), sufficientStockLedgerId);
        assertThat(unconfigured.getSafetyQuantity()).isZero();
        assertThat(unconfigured.getWarningEnabled()).isFalse();
        assertThat(unconfigured.getInWarningPeriod()).isFalse();
    }

    @Test
    void upsert_enablesWarningWhenStockBelowSafety() {
        SafetyStockUpdateDTO dto = new SafetyStockUpdateDTO();
        dto.setSafetyQuantity(10);
        dto.setWarningEnabled(true);

        SafetyStockRecordVO updated = safetyStockService.upsert(lowStockLedgerId, dto);

        assertThat(updated.getSafetyQuantity()).isEqualTo(10);
        assertThat(updated.getWarningEnabled()).isTrue();
        assertThat(updated.getInWarningPeriod()).isTrue();
    }

    @Test
    void upsert_warningDisabledEvenWhenStockBelowSafety() {
        SafetyStockUpdateDTO dto = new SafetyStockUpdateDTO();
        dto.setSafetyQuantity(10);
        dto.setWarningEnabled(false);

        SafetyStockRecordVO updated = safetyStockService.upsert(lowStockLedgerId, dto);

        assertThat(updated.getInWarningPeriod()).isFalse();
    }

    @Test
    void page_warningPeriodFilterYes() {
        upsert(lowStockLedgerId, 10, true);
        upsert(sufficientStockLedgerId, 10, true);

        SafetyStockQueryDTO query = new SafetyStockQueryDTO();
        query.setWarningPeriod("是");

        var result = safetyStockService.page(query);
        assertThat(result.getRecords()).allMatch(vo -> Boolean.TRUE.equals(vo.getInWarningPeriod()));
        assertThat(result.getRecords()).extracting(SafetyStockRecordVO::getMaterialLedgerId)
                .contains(lowStockLedgerId)
                .doesNotContain(sufficientStockLedgerId);
    }

    @Test
    void page_safetyQuantityKeywordFilter() {
        upsert(lowStockLedgerId, 108, true);
        upsert(sufficientStockLedgerId, 20, true);

        SafetyStockQueryDTO query = new SafetyStockQueryDTO();
        query.setSafetyQuantityKeyword("08");

        List<SafetyStockRecordVO> records = safetyStockService.listByQuery(query);
        assertThat(records).extracting(SafetyStockRecordVO::getMaterialLedgerId)
                .containsExactly(lowStockLedgerId);
    }

    private void upsert(Long ledgerId, int safetyQuantity, boolean warningEnabled) {
        SafetyStockUpdateDTO dto = new SafetyStockUpdateDTO();
        dto.setSafetyQuantity(safetyQuantity);
        dto.setWarningEnabled(warningEnabled);
        safetyStockService.upsert(ledgerId, dto);
    }

    private Long insertLedger(String name, int stockQuantity) {
        MaterialLedger ledger = new MaterialLedger();
        ledger.setCategory("耗材");
        ledger.setGenericName("测试统称");
        ledger.setBrand("品牌");
        ledger.setName(name);
        ledger.setModel("M-" + name);
        ledger.setBinLocation("1-1-1");
        ledger.setStockQuantity(stockQuantity);
        materialLedgerMapper.insert(ledger);
        return ledger.getId();
    }

    private SafetyStockRecordVO findByLedgerId(List<SafetyStockRecordVO> records, Long ledgerId) {
        return records.stream()
                .filter(record -> ledgerId.equals(record.getMaterialLedgerId()))
                .findFirst()
                .orElseThrow();
    }
}
