package com.storage.warehouse.service;

import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.dto.SafetyStockQueryDTO;
import com.storage.warehouse.dto.SafetyStockRecordVO;
import com.storage.warehouse.dto.SafetyStockUpdateDTO;
import com.storage.warehouse.mapper.SafetyStockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SafetyStockServiceIntegrationTest {

    @Autowired
    private SafetyStockService safetyStockService;

    @Autowired
    private MaterialIoRecordMapper materialIoRecordMapper;

    @Autowired
    private MaterialLedgerMapper materialLedgerMapper;

    @Autowired
    private SafetyStockMapper safetyStockMapper;

    private Long lowStockLedgerId;
    private Long sufficientStockLedgerId;
    private Long equalStockLedgerId;

    @BeforeEach
    void setUp() {
        materialIoRecordMapper.delete(null);
        safetyStockMapper.delete(null);
        materialLedgerMapper.delete(null);

        lowStockLedgerId = insertLedger("低库存物料", 5);
        sufficientStockLedgerId = insertLedger("充足库存物料", 50);
        equalStockLedgerId = insertLedger("相等库存物料", 10);
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

        SafetyStockRecordVO updated = safetyStockService.upsert(lowStockLedgerId, dto);

        assertThat(updated.getSafetyQuantity()).isEqualTo(10);
        assertThat(updated.getWarningEnabled()).isTrue();
        assertThat(updated.getInWarningPeriod()).isTrue();
    }

    @Test
    void upsert_warnsWhenStockEqualsSafety() {
        SafetyStockUpdateDTO dto = new SafetyStockUpdateDTO();
        dto.setSafetyQuantity(10);

        SafetyStockRecordVO updated = safetyStockService.upsert(equalStockLedgerId, dto);

        assertThat(updated.getInWarningPeriod()).isTrue();
    }

    @Test
    void upsert_ignoresManualWarningDisabledFlag() {
        SafetyStockUpdateDTO dto = new SafetyStockUpdateDTO();
        dto.setSafetyQuantity(10);
        dto.setWarningEnabled(false);

        SafetyStockRecordVO updated = safetyStockService.upsert(lowStockLedgerId, dto);

        assertThat(updated.getWarningEnabled()).isTrue();
        assertThat(updated.getInWarningPeriod()).isTrue();
    }

    @Test
    void upsert_zeroSafetyQuantityDoesNotWarn() {
        SafetyStockUpdateDTO dto = new SafetyStockUpdateDTO();
        dto.setSafetyQuantity(0);

        SafetyStockRecordVO updated = safetyStockService.upsert(lowStockLedgerId, dto);

        assertThat(updated.getWarningEnabled()).isFalse();
        assertThat(updated.getInWarningPeriod()).isFalse();
    }

    @Test
    void page_warningPeriodFilterYes() {
        upsert(lowStockLedgerId, 10);
        upsert(sufficientStockLedgerId, 10);

        SafetyStockQueryDTO query = new SafetyStockQueryDTO();
        query.setWarningPeriod("YES");

        var result = safetyStockService.page(query);
        assertThat(result.getRecords()).allMatch(vo -> Boolean.TRUE.equals(vo.getInWarningPeriod()));
        assertThat(result.getRecords()).extracting(SafetyStockRecordVO::getMaterialLedgerId)
                .contains(lowStockLedgerId)
                .doesNotContain(sufficientStockLedgerId);
    }

    @Test
    void page_warningItemsSortedFirst() {
        upsert(lowStockLedgerId, 10);
        upsert(sufficientStockLedgerId, 10);
        upsert(equalStockLedgerId, 10);

        var result = safetyStockService.page(new SafetyStockQueryDTO());
        List<SafetyStockRecordVO> records = result.getRecords();

        assertThat(records).hasSize(3);
        assertThat(records.subList(0, 2))
                .allMatch(record -> Boolean.TRUE.equals(record.getInWarningPeriod()));
        assertThat(records.get(2).getInWarningPeriod()).isFalse();
        assertThat(records.get(2).getMaterialLedgerId()).isEqualTo(sufficientStockLedgerId);
    }

    @Test
    void page_safetyQuantityKeywordFilter() {
        upsert(lowStockLedgerId, 108);
        upsert(sufficientStockLedgerId, 20);

        SafetyStockQueryDTO query = new SafetyStockQueryDTO();
        query.setSafetyQuantityKeyword("08");

        List<SafetyStockRecordVO> records = safetyStockService.listByQuery(query);
        assertThat(records).extracting(SafetyStockRecordVO::getMaterialLedgerId)
                .containsExactly(lowStockLedgerId);
    }

    @Test
    void page_rejectsInvalidWarningPeriod() {
        SafetyStockQueryDTO query = new SafetyStockQueryDTO();
        query.setWarningPeriod("INVALID");

        assertThatThrownBy(() -> safetyStockService.page(query))
                .getRootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("预警状态无效");
    }

    private void upsert(Long ledgerId, int safetyQuantity) {
        SafetyStockUpdateDTO dto = new SafetyStockUpdateDTO();
        dto.setSafetyQuantity(safetyQuantity);
        safetyStockService.upsert(ledgerId, dto);
    }

    private Long insertLedger(String name, int stockQuantity) {
        MaterialLedger ledger = new MaterialLedger();
        ledger.setCategory("电子");
        ledger.setGenericName("测试统称");
        ledger.setBrand("测试品牌");
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
