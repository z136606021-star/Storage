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
        WarehouseBin created = warehouseBinService.create(binDto(1, 2, 3));

        assertThat(created.getBinCode()).isEqualTo("1-2-3");
    }

    @Test
    void create_generatesRowOnlyBinCode() {
        WarehouseBin created = warehouseBinService.create(binDto(5, null, null));

        assertThat(created.getBinCode()).isEqualTo("5");
        assertThat(created.getColNo()).isNull();
        assertThat(created.getLevelNo()).isNull();
    }

    @Test
    void create_generatesRowColBinCode() {
        WarehouseBin created = warehouseBinService.create(binDto(2, 4, null));

        assertThat(created.getBinCode()).isEqualTo("2-4");
        assertThat(created.getLevelNo()).isNull();
    }

    @Test
    void create_rejectsLevelWithoutColumn() {
        WarehouseBinSaveDTO dto = binDto(1, null, 2);

        assertThatThrownBy(() -> warehouseBinService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("填写层时必须同时填写列");
    }

    @Test
    void duplicateBinCode_rejects() {
        warehouseBinService.create(binDto(1, null, null));

        assertThatThrownBy(() -> warehouseBinService.create(binDto(1, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bin位编号已存在");
    }

    @Test
    void create_recordsOperator() {
        WarehouseBin created = warehouseBinService.create(binDto(1, null, null));

        assertThat(created.getOperatorUserId()).isEqualTo(1L);
        assertThat(created.getOperatorName()).isEqualTo("tester");
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_recordsOperator() {
        WarehouseBin created = warehouseBinService.create(binDto(1, null, null));

        OperatorInfo updater = new OperatorInfo();
        updater.setId(1L);
        updater.setUsername("editor");
        when(operatorResolver.requireCurrentOperator()).thenReturn(updater);

        WarehouseBinSaveDTO dto = binDto(1, null, null);
        dto.setRemark("updated");

        WarehouseBin updated = warehouseBinService.update(created.getId(), dto);

        assertThat(updated.getOperatorUserId()).isEqualTo(1L);
        assertThat(updated.getOperatorName()).isEqualTo("editor");
        assertThat(updated.getRemark()).isEqualTo("updated");
    }

    @Test
    void page_sortsByUpdatedAtDescThenIdDesc() {
        warehouseBinService.create(binDto(1, null, null));
        WarehouseBin newer = warehouseBinService.create(binDto(2, null, null));

        newer.setUpdatedAt(java.time.LocalDateTime.now().plusMinutes(5));
        warehouseBinMapper.updateById(newer);

        var page = warehouseBinService.page(new WarehouseBinQueryDTO());
        List<WarehouseBin> records = page.getRecords();

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getBinCode()).isEqualTo("2");
        assertThat(records.get(1).getBinCode()).isEqualTo("1");
    }

    @Test
    void page_tieBreaksEqualUpdatedAtByIdDesc() {
        WarehouseBin first = warehouseBinService.create(binDto(1, null, null));
        WarehouseBin second = warehouseBinService.create(binDto(2, null, null));

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
            warehouseBinService.create(binDto(i, null, null));
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
        WarehouseBin first = warehouseBinService.create(binDto(1, null, null));
        warehouseBinService.create(binDto(2, null, null));

        WarehouseBinQueryDTO query = new WarehouseBinQueryDTO();
        query.setIds(List.of(first.getId()));

        assertThat(warehouseBinService.listByQuery(query))
                .extracting(WarehouseBin::getBinCode)
                .containsExactly("1");
    }

    @Test
    void page_filtersByBinCodeKeyword() {
        warehouseBinService.create(binDto(1, 2, null));
        warehouseBinService.create(binDto(9, null, null));

        WarehouseBinQueryDTO query = new WarehouseBinQueryDTO();
        query.setBinCode("1-2");

        assertThat(warehouseBinService.page(query).getRecords())
                .extracting(WarehouseBin::getBinCode)
                .containsExactly("1-2");
    }

    private WarehouseBinSaveDTO binDto(Integer row, Integer col, Integer level) {
        WarehouseBinSaveDTO dto = new WarehouseBinSaveDTO();
        dto.setRowNo(row);
        dto.setColNo(col);
        dto.setLevelNo(level);
        return dto;
    }
}
