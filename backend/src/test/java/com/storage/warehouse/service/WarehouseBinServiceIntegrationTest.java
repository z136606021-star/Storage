package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;
import com.storage.warehouse.dto.WarehouseBinQueryDTO;
import com.storage.warehouse.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
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
class WarehouseBinServiceIntegrationTest {

    @Autowired
    private WarehouseBinService warehouseBinService;

    @Autowired
    private WarehouseBinMapper warehouseBinMapper;

    @MockBean
    private OperatorResolver operatorResolver;

    @BeforeEach
    void setUp() {
        warehouseBinMapper.delete(null);

        OperatorInfo operator = new OperatorInfo();
        operator.setId(1L);
        operator.setUsername("tester");
        when(operatorResolver.requireCurrentOperator()).thenReturn(operator);
    }

    @Test
    void create_generatesFullBinCode() {
        WarehouseBin created = warehouseBinService.create(binDto("A", 2, 3));

        assertThat(created.getBinCode()).isEqualTo("A-2-3");
    }

    @Test
    void create_generatesRowOnlyBinCode() {
        WarehouseBin created = warehouseBinService.create(binDto("  铁柜  ", null, null));

        assertThat(created.getBinCode()).isEqualTo("铁柜");
        assertThat(created.getRowNo()).isEqualTo("铁柜");
        assertThat(created.getColNo()).isNull();
        assertThat(created.getLevelNo()).isNull();
    }

    @Test
    void create_generatesRowColBinCode() {
        WarehouseBin created = warehouseBinService.create(binDto("B", 4, null));

        assertThat(created.getBinCode()).isEqualTo("B-4");
        assertThat(created.getLevelNo()).isNull();
    }

    @Test
    void create_rejectsLevelWithoutColumn() {
        WarehouseBinSaveDTO dto = binDto("A", null, 2);

        assertThatThrownBy(() -> warehouseBinService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("填写层时必须同时填写列");
    }

    @Test
    void duplicateBinCode_rejects() {
        warehouseBinService.create(binDto("A", null, null));

        assertThatThrownBy(() -> warehouseBinService.create(binDto("A", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bin位编号已存在");
    }

    @Test
    void create_recordsOperator() {
        WarehouseBin created = warehouseBinService.create(binDto("A", null, null));

        assertThat(created.getOperatorUserId()).isEqualTo(1L);
        assertThat(created.getOperatorName()).isEqualTo("tester");
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_recordsOperator() {
        WarehouseBin created = warehouseBinService.create(binDto("A", null, null));

        OperatorInfo updater = new OperatorInfo();
        updater.setId(1L);
        updater.setUsername("editor");
        when(operatorResolver.requireCurrentOperator()).thenReturn(updater);

        WarehouseBinSaveDTO dto = binDto("A", null, null);
        dto.setRemark("updated");

        WarehouseBin updated = warehouseBinService.update(created.getId(), dto);

        assertThat(updated.getOperatorUserId()).isEqualTo(1L);
        assertThat(updated.getOperatorName()).isEqualTo("editor");
        assertThat(updated.getRemark()).isEqualTo("updated");
    }

    @Test
    void page_sortsByUpdatedAtDescThenIdDesc() {
        warehouseBinService.create(binDto("A", null, null));
        WarehouseBin newer = warehouseBinService.create(binDto("B", null, null));

        newer.setUpdatedAt(java.time.LocalDateTime.now().plusMinutes(5));
        warehouseBinMapper.updateById(newer);

        var page = warehouseBinService.page(new WarehouseBinQueryDTO());
        List<WarehouseBin> records = page.getRecords();

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getBinCode()).isEqualTo("B");
        assertThat(records.get(1).getBinCode()).isEqualTo("A");
    }

    @Test
    void page_tieBreaksEqualUpdatedAtByIdDesc() {
        WarehouseBin first = warehouseBinService.create(binDto("A", null, null));
        WarehouseBin second = warehouseBinService.create(binDto("B", null, null));

        java.time.LocalDateTime sameTime = java.time.LocalDateTime.now();
        first.setUpdatedAt(sameTime);
        second.setUpdatedAt(sameTime);
        warehouseBinMapper.updateById(first);
        warehouseBinMapper.updateById(second);

        var page = warehouseBinService.page(new WarehouseBinQueryDTO());

        assertThat(page.getRecords())
                .extracting(WarehouseBin::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void page_returnsPaginationMetadata() {
        for (int i = 1; i <= 12; i++) {
            warehouseBinService.create(binDto(String.valueOf(i), null, null));
        }

        WarehouseBinQueryDTO query = new WarehouseBinQueryDTO();
        query.setPage(2);
        query.setPageSize(10);

        var page = warehouseBinService.page(query);

        assertThat(page.getTotal()).isEqualTo(12);
        assertThat(page.getCurrent()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getRecords()).hasSize(2);
    }

    @Test
    void listByIds_returnsOnlySelectedRecords() {
        WarehouseBin first = warehouseBinService.create(binDto("A", null, null));
        warehouseBinService.create(binDto("B", null, null));

        WarehouseBinQueryDTO query = new WarehouseBinQueryDTO();
        query.setIds(List.of(first.getId()));

        assertThat(warehouseBinService.listByQuery(query))
                .extracting(WarehouseBin::getBinCode)
                .containsExactly("A");
    }

    @Test
    void page_filtersByBinCodeKeyword() {
        warehouseBinService.create(binDto("A", 2, null));
        warehouseBinService.create(binDto("Z", null, null));

        WarehouseBinQueryDTO query = new WarehouseBinQueryDTO();
        query.setBinCode("A-2");

        assertThat(warehouseBinService.page(query).getRecords())
                .extracting(WarehouseBin::getBinCode)
                .containsExactly("A-2");
    }

    private WarehouseBinSaveDTO binDto(String row, Integer col, Integer level) {
        WarehouseBinSaveDTO dto = new WarehouseBinSaveDTO();
        dto.setRowNo(row);
        dto.setColNo(col);
        dto.setLevelNo(level);
        return dto;
    }
}
