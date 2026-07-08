package com.storage.warehouse.bom.service;

import com.storage.warehouse.bom.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.bom.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.bom.mapper.WarehouseBomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WarehouseBomServiceIntegrationTest {

    @Autowired
    private WarehouseBomService warehouseBomService;

    @Autowired
    private WarehouseBomMapper warehouseBomMapper;

    @BeforeEach
    void setUp() {
        warehouseBomMapper.delete(null);
    }

    @Test
    void page_enrichesImageObjectKeyWithSameOriginPreviewUrl() {
        WarehouseBomSaveDTO dto = new WarehouseBomSaveDTO();
        dto.setCategory("耗材");
        dto.setGenericName("密封圈");
        dto.setBrand("三环");
        dto.setName("O型密封圈");
        dto.setModel("OR-10");
        dto.setImageObjectKey("2026-07-08/demo image.png");
        warehouseBomService.create(dto);

        WarehouseBomQueryDTO query = new WarehouseBomQueryDTO();
        query.setPage(1);
        query.setPageSize(10);

        var page = warehouseBomService.page(query);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getImageUrl())
                .isEqualTo("/api/files/preview?objectKey=2026-07-08%2Fdemo+image.png");
    }
}
