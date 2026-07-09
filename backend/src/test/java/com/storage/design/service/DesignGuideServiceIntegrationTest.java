package com.storage.design.service;

import com.storage.common.exception.BusinessException;
import com.storage.design.dto.DesignGuideQueryDTO;
import com.storage.design.dto.DesignGuideSaveDTO;
import com.storage.design.dto.DesignProductTypeSaveDTO;
import com.storage.design.dto.DesignStageSaveDTO;
import com.storage.design.entity.DesignProductType;
import com.storage.design.entity.DesignStage;
import com.storage.design.mapper.DesignGuideMapper;
import com.storage.design.mapper.DesignProductTypeMapper;
import com.storage.design.mapper.DesignStageMapper;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DesignGuideServiceIntegrationTest {

    @Autowired
    private DesignGuideService designGuideService;

    @Autowired
    private DesignProductTypeService productTypeService;

    @Autowired
    private DesignStageService stageService;

    @Autowired
    private DesignGuideMapper designGuideMapper;

    @Autowired
    private DesignProductTypeMapper productTypeMapper;

    @Autowired
    private DesignStageMapper stageMapper;

    @MockBean
    private OperatorResolver operatorResolver;

    @BeforeEach
    void setUp() {
        designGuideMapper.delete(null);
        productTypeMapper.delete(null);
        stageMapper.delete(null);

        OperatorInfo operator = new OperatorInfo();
        operator.setId(1L);
        operator.setUsername("tester");
        when(operatorResolver.requireCurrentOperator()).thenReturn(operator);
    }

    @Test
    void createAndPageDesignGuide() {
        DesignProductType productType = productTypeService.create(productTypeDto("A01", "机器人", true));
        DesignStage stage = stageService.create(stageDto(1, "设计", true));

        var created = designGuideService.create(guideDto(productType.getId(), stage.getId(), "Common", "线缆位置确认"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getProductTypeCode()).isEqualTo("A01");
        assertThat(created.getProductTypeName()).isEqualTo("机器人");
        assertThat(created.getStageName()).isEqualTo("设计");

        DesignGuideQueryDTO query = new DesignGuideQueryDTO();
        query.setProductTypeId(productType.getId());
        var page = designGuideService.page(query);
        assertThat(page.getRecords()).hasSize(1);
    }

    @Test
    void duplicateProductTypeCode_rejects() {
        productTypeService.create(productTypeDto("A01", "机器人", true));

        assertThatThrownBy(() -> productTypeService.create(productTypeDto("A01", "重复", true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品类型编号已存在");
    }

    @Test
    void referencedProductType_cannotDelete() {
        DesignProductType productType = productTypeService.create(productTypeDto("A01", "机器人", true));
        DesignStage stage = stageService.create(stageDto(1, "设计", true));
        designGuideService.create(guideDto(productType.getId(), stage.getId(), "Common", "线缆位置确认"));

        assertThatThrownBy(() -> productTypeService.delete(productType.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被 1 条设计指引引用");
    }

    @Test
    void disabledConfig_cannotCreateGuide() {
        DesignProductType productType = productTypeService.create(productTypeDto("A01", "机器人", false));
        DesignStage stage = stageService.create(stageDto(1, "设计", true));

        assertThatThrownBy(() -> designGuideService.create(
                guideDto(productType.getId(), stage.getId(), "Common", "线缆位置确认")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品类型已停用");
    }

    @Test
    void importExcel_withUnknownProductType_returnsErrorRow() throws Exception {
        MockMultipartFile file = workbookFile("UNKNOWN", "不存在", "设计", "Common", "检查项");

        var result = designGuideService.importExcel(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("未找到启用的产品类型");
    }

    private DesignProductTypeSaveDTO productTypeDto(String code, String name, boolean enabled) {
        DesignProductTypeSaveDTO dto = new DesignProductTypeSaveDTO();
        dto.setTypeCode(code);
        dto.setTypeName(name);
        dto.setEnabled(enabled);
        return dto;
    }

    private DesignStageSaveDTO stageDto(int sortOrder, String name, boolean enabled) {
        DesignStageSaveDTO dto = new DesignStageSaveDTO();
        dto.setSortOrder(sortOrder);
        dto.setStageName(name);
        dto.setEnabled(enabled);
        return dto;
    }

    private DesignGuideSaveDTO guideDto(Long productTypeId, Long stageId, String scope, String checkItem) {
        DesignGuideSaveDTO dto = new DesignGuideSaveDTO();
        dto.setProductTypeId(productTypeId);
        dto.setStageId(stageId);
        dto.setScope(scope);
        dto.setCheckItem(checkItem);
        return dto;
    }

    private MockMultipartFile workbookFile(
            String typeCode,
            String typeName,
            String stageName,
            String scope,
            String checkItem
    ) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("设计指引导入");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("产品类型编号");
            header.createCell(1).setCellValue("产品类型");
            header.createCell(2).setCellValue("阶段");
            header.createCell(3).setCellValue("适用范围");
            header.createCell(4).setCellValue("检查项");
            header.createCell(5).setCellValue("备注");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(typeCode);
            row.createCell(1).setCellValue(typeName);
            row.createCell(2).setCellValue(stageName);
            row.createCell(3).setCellValue(scope);
            row.createCell(4).setCellValue(checkItem);
            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "design-guides.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        }
    }
}
