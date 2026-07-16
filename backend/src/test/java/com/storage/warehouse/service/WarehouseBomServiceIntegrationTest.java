package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;
import com.storage.infrastructure.file.service.FileStorageService;
import com.storage.warehouse.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.mapper.WarehouseBomImageMapper;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class WarehouseBomServiceIntegrationTest {

    @Autowired
    private WarehouseBomService warehouseBomService;

    @Autowired
    private WarehouseBomMapper warehouseBomMapper;

    @Autowired
    private WarehouseBomImageMapper warehouseBomImageMapper;

    @MockBean
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        warehouseBomImageMapper.delete(null);
        warehouseBomMapper.delete(null);
        when(fileStorageService.resolveAccessUrl("2026-07-08/demo image.png"))
                .thenReturn("/api/files/preview?objectKey=2026-07-08%2Fdemo+image.png");
        when(fileStorageService.resolveAccessUrl("2026-07-08/demo image-2.png"))
                .thenReturn("/api/files/preview?objectKey=2026-07-08%2Fdemo+image-2.png");
    }

    @Test
    void page_enrichesMultipleImagePreviewUrls() {
        WarehouseBomSaveDTO dto = new WarehouseBomSaveDTO();
        dto.setCategory("耗材");
        dto.setGenericName("密封圈");
        dto.setBrand("三环");
        dto.setName("O型密封圈");
        dto.setImageObjectKeys(List.of(
                "2026-07-08/demo image.png",
                "2026-07-08/demo image-2.png"
        ));
        warehouseBomService.create(dto);

        WarehouseBomQueryDTO query = new WarehouseBomQueryDTO();
        query.setPage(1);
        query.setPageSize(10);

        var page = warehouseBomService.page(query);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getImageUrls())
                .containsExactly(
                        "/api/files/preview?objectKey=2026-07-08%2Fdemo+image.png",
                        "/api/files/preview?objectKey=2026-07-08%2Fdemo+image-2.png"
                );
    }

    @Test
    void create_rejectsDuplicateNaturalKey() {
        WarehouseBomSaveDTO first = baseDto();
        warehouseBomService.create(first);

        WarehouseBomSaveDTO duplicate = baseDto();
        assertThatThrownBy(() -> warehouseBomService.create(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("相同品类/统称/品牌/名称");
    }

    @Test
    void assertCatalogExists_matchesFourFieldKey() {
        warehouseBomService.create(baseDto());

        warehouseBomService.assertCatalogExists("耗材", "密封圈", "三环", "O型密封圈");

        assertThatThrownBy(() -> warehouseBomService.assertCatalogExists(
                "耗材", "密封圈", "三环", "不存在"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("物料清单中不存在");
    }

    @Test
    void page_ordersByUpdatedAtDesc() throws InterruptedException {
        warehouseBomService.create(baseDto());
        Thread.sleep(20);
        WarehouseBomSaveDTO second = baseDto();
        second.setName("O型密封圈-2");
        warehouseBomService.create(second);

        var page = warehouseBomService.page(new WarehouseBomQueryDTO());

        assertThat(page.getRecords()).extracting("name")
                .containsExactly("O型密封圈-2", "O型密封圈");
    }

    @Test
    void create_rejectsUnknownImageObjectKey() {
        WarehouseBomSaveDTO dto = baseDto();
        dto.setImageObjectKeys(List.of("2026-07-08/missing.png"));
        doThrow(new BusinessException("图片文件不存在或类型不支持: 2026-07-08/missing.png"))
                .when(fileStorageService).assertImageFile("2026-07-08/missing.png");

        assertThatThrownBy(() -> warehouseBomService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片文件不存在或类型不支持");
    }

    private WarehouseBomSaveDTO baseDto() {
        WarehouseBomSaveDTO dto = new WarehouseBomSaveDTO();
        dto.setCategory("耗材");
        dto.setGenericName("密封圈");
        dto.setBrand("三环");
        dto.setName("O型密封圈");
        return dto;
    }
}
