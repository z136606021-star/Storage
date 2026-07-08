package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;
import com.storage.warehouse.dto.WarehouseBinQueryDTO;
import com.storage.warehouse.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class WarehouseBinServiceIntegrationTest {

    @Autowired
    private WarehouseBinService warehouseBinService;

    @Autowired
    private WarehouseBinMapper warehouseBinMapper;

    @BeforeEach
    void setUp() {
        warehouseBinMapper.delete(null);
    }

    @Test
    void createAndPageBin() {
        WarehouseBinSaveDTO dto = new WarehouseBinSaveDTO();
        dto.setRowNo(1);
        dto.setColNo(2);
        dto.setLevelNo(3);

        var created = warehouseBinService.create(dto);
        assertThat(created.getBinCode()).isEqualTo("1-2-3");

        WarehouseBinQueryDTO query = new WarehouseBinQueryDTO();
        query.setPage(1);
        query.setPageSize(10);

        var page = warehouseBinService.page(query);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void duplicateBinCode_rejects() {
        warehouseBinService.create(binDto(1, 1, 1));

        assertThatThrownBy(() -> warehouseBinService.create(binDto(1, 1, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Bin位编号已存在");
    }

    private WarehouseBinSaveDTO binDto(int row, int col, int level) {
        WarehouseBinSaveDTO dto = new WarehouseBinSaveDTO();
        dto.setRowNo(row);
        dto.setColNo(col);
        dto.setLevelNo(level);
        return dto;
    }
}
