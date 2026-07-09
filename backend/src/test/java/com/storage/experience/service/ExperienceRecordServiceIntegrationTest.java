package com.storage.experience.service;

import com.storage.common.exception.BusinessException;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.experience.dto.ExperienceRecordQueryDTO;
import com.storage.experience.dto.ExperienceRecordSaveDTO;
import com.storage.experience.dto.ExperienceTypeSaveDTO;
import com.storage.experience.entity.ExperienceType;
import com.storage.experience.excel.ExperienceExportRow;
import com.storage.experience.mapper.ExperienceAttachmentMapper;
import com.storage.experience.mapper.ExperienceProjectLinkMapper;
import com.storage.experience.mapper.ExperienceRecordMapper;
import com.storage.experience.mapper.ExperienceTypeMapper;
import com.storage.infrastructure.file.entity.SysFile;
import com.storage.infrastructure.file.mapper.SysFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ExperienceRecordServiceIntegrationTest {

    @Autowired
    private ExperienceRecordService experienceRecordService;

    @Autowired
    private ExperienceRecordImportService experienceRecordImportService;

    @Autowired
    private ExperienceTypeService experienceTypeService;

    @Autowired
    private ExperienceRecordMapper experienceRecordMapper;

    @Autowired
    private ExperienceTypeMapper experienceTypeMapper;

    @Autowired
    private ExperienceProjectLinkMapper experienceProjectLinkMapper;

    @Autowired
    private ExperienceAttachmentMapper experienceAttachmentMapper;

    @Autowired
    private SysFileMapper sysFileMapper;

    private ExperienceType type;

    @BeforeEach
    void setUp() {
        experienceAttachmentMapper.delete(null);
        experienceProjectLinkMapper.delete(null);
        experienceRecordMapper.delete(null);
        experienceTypeMapper.delete(null);
        sysFileMapper.delete(null);

        ExperienceTypeSaveDTO dto = new ExperienceTypeSaveDTO();
        dto.setName("客户需求变更");
        dto.setStatus(1);
        dto.setSortOrder(10);
        type = experienceTypeService.create(dto);
    }

    @Test
    void createAndPageExperienceRecord() {
        var created = experienceRecordService.createImported(saveDto("客户需求频繁变更"), "荣云");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTypeName()).isEqualTo("客户需求变更");
        assertThat(created.getProjectNames()).containsExactly("TCU TLA Noise and EOL Automation - 0612");

        ExperienceRecordQueryDTO query = new ExperienceRecordQueryDTO();
        query.setKeyword("频繁");
        query.setPage(1);
        query.setPageSize(10);

        var page = experienceRecordService.page(query);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getRecorderName()).isEqualTo("荣云");
    }

    @Test
    void updateExperienceRecord_changesSuggestion() {
        var created = experienceRecordService.createImported(saveDto("机械手臂电路设计"), "邱泳");

        ExperienceRecordSaveDTO update = saveDto("机械手臂电路设计");
        update.setSuggestion("提前评审接口定义");
        var updated = experienceRecordService.update(created.getId(), update);

        assertThat(updated.getSuggestion()).isEqualTo("提前评审接口定义");
    }

    @Test
    void createWithAttachment_linksFileMetadata() {
        SysFile file = new SysFile();
        file.setObjectKey("2026-07-08/demo.pdf");
        file.setOriginalName("demo.pdf");
        file.setContentType("application/pdf");
        file.setSizeBytes(128L);
        sysFileMapper.insert(file);

        ExperienceRecordSaveDTO dto = saveDto("波峰托盘使用后定位柱下降");
        dto.setAttachmentObjectKeys(List.of(file.getObjectKey()));
        var created = experienceRecordService.createImported(dto, "林智功");

        assertThat(created.getAttachments()).hasSize(1);
        assertThat(created.getAttachments().get(0).getOriginalName()).isEqualTo("demo.pdf");
        assertThat(created.getAttachments().get(0).getUrl()).contains("/api/files/download");
    }

    @Test
    void deleteType_whenReferenced_rejects() {
        experienceRecordService.createImported(saveDto("客户需求频繁变更"), "荣云");

        assertThatThrownBy(() -> experienceTypeService.delete(type.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被经验记录引用");
    }

    @Test
    void importExcel_withValidRow_succeeds() throws Exception {
        ExperienceExportRow row = new ExperienceExportRow();
        row.setTypeName("客户需求变更");
        row.setDescription("导入经验描述");
        row.setRecorderName("导入人");
        byte[] content = AutoPoiExcelTemplate.exportBytes("经验库", ExperienceExportRow.class, new ArrayList<>(List.of(row)));

        var result = experienceRecordImportService.importExcel(new MockMultipartFile(
                "file",
                "experience.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        ));

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isZero();
    }

    @Test
    void importExcel_withUnknownType_returnsRowError() throws Exception {
        ExperienceExportRow row = new ExperienceExportRow();
        row.setTypeName("未知类型");
        row.setDescription("导入经验描述");
        byte[] content = AutoPoiExcelTemplate.exportBytes("经验库", ExperienceExportRow.class, new ArrayList<>(List.of(row)));

        var result = experienceRecordImportService.importExcel(new MockMultipartFile(
                "file",
                "experience.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        ));

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("类型不存在");
    }

    private ExperienceRecordSaveDTO saveDto(String description) {
        ExperienceRecordSaveDTO dto = new ExperienceRecordSaveDTO();
        dto.setTypeId(type.getId());
        dto.setDescription(description);
        dto.setImpact("影响交付节奏");
        dto.setSuggestion("提前冻结需求");
        dto.setActionPlan("建立变更评审");
        dto.setProjectNames(List.of("TCU TLA Noise and EOL Automation - 0612"));
        return dto;
    }
}
