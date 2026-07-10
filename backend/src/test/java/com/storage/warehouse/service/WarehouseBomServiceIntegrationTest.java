package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;
import com.storage.infrastructure.file.entity.SysFile;
import com.storage.infrastructure.file.mapper.SysFileMapper;
import com.storage.warehouse.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.mapper.WarehouseBomImageMapper;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class WarehouseBomServiceIntegrationTest {

    @Autowired
    private WarehouseBomService warehouseBomService;

    @Autowired
    private WarehouseBomMapper warehouseBomMapper;

    @Autowired
    private WarehouseBomImageMapper warehouseBomImageMapper;

    @Autowired
    private SysFileMapper sysFileMapper;

    @BeforeEach
    void setUp() {
        warehouseBomImageMapper.delete(null);
        warehouseBomMapper.delete(null);
    }

    @Test
    void page_enrichesMultipleImagePreviewUrls() {
        insertImageFile("2026-07-08/demo image.png");
        insertImageFile("2026-07-08/demo image-2.png");
        WarehouseBomSaveDTO dto = new WarehouseBomSaveDTO();
        dto.setCategory("耗材");
        dto.setGenericName("密封圈");
        dto.setBrand("三环");
        dto.setName("O型密封圈");
        dto.setModel("OR-10");
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
    void create_rejectsDuplicateNaturalKeyIgnoringModel() {
        WarehouseBomSaveDTO first = baseDto("OR-10");
        warehouseBomService.create(first);

        WarehouseBomSaveDTO duplicate = baseDto("OR-20");
        assertThatThrownBy(() -> warehouseBomService.create(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("相同品类/统称/品牌/名称");
    }

    @Test
    void assertCatalogExists_matchesFourFieldKeyAndValidatesModel() {
        warehouseBomService.create(baseDto("OR-10"));

        warehouseBomService.assertCatalogExists("耗材", "密封圈", "三环", "O型密封圈", "OR-10");

        assertThatThrownBy(() -> warehouseBomService.assertCatalogExists(
                "耗材", "密封圈", "三环", "O型密封圈", "OR-99"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("规格不匹配");
    }

    @Test
    void page_ordersByUpdatedAtDesc() throws InterruptedException {
        warehouseBomService.create(baseDto("OR-10"));
        Thread.sleep(20);
        WarehouseBomSaveDTO second = baseDto("OR-20");
        second.setName("O型密封圈-2");
        warehouseBomService.create(second);

        var page = warehouseBomService.page(new WarehouseBomQueryDTO());

        assertThat(page.getRecords()).extracting("name")
                .containsExactly("O型密封圈-2", "O型密封圈");
    }

    @Test
    void create_rejectsUnknownImageObjectKey() {
        WarehouseBomSaveDTO dto = baseDto("OR-10");
        dto.setImageObjectKeys(List.of("2026-07-08/missing.png"));

        assertThatThrownBy(() -> warehouseBomService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片文件不存在或类型不支持");
    }

    @Test
    void create_rejectsMoreThanTwentyImages() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            String key = "2026-07-10/image-" + i + ".png";
            insertImageFile(key);
            keys.add(key);
        }

        WarehouseBomSaveDTO dto = baseDto("OR-10");
        dto.setImageObjectKeys(keys);

        assertThatThrownBy(() -> warehouseBomService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片数量不能超过20张");
    }

    private void insertImageFile(String objectKey) {
        SysFile file = new SysFile();
        file.setObjectKey(objectKey);
        file.setOriginalName(objectKey.substring(objectKey.lastIndexOf('/') + 1));
        file.setContentType("image/png");
        file.setSizeBytes(128L);
        sysFileMapper.insert(file);
    }

    private WarehouseBomSaveDTO baseDto(String model) {
        WarehouseBomSaveDTO dto = new WarehouseBomSaveDTO();
        dto.setCategory("耗材");
        dto.setGenericName("密封圈");
        dto.setBrand("三环");
        dto.setName("O型密封圈");
        dto.setModel(model);
        return dto;
    }
}
