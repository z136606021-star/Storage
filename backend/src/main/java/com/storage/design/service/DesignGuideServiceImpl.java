package com.storage.design.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.ImportResultVO;
import com.storage.common.dto.PageResult;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.common.exception.BusinessException;
import com.storage.common.query.PageSupport;
import com.storage.design.converter.DesignGuideConverter;
import com.storage.design.dto.DesignGuideFilterOptionsVO;
import com.storage.design.dto.DesignGuideQueryDTO;
import com.storage.design.dto.DesignGuideSaveDTO;
import com.storage.design.dto.DesignProductTypeOptionVO;
import com.storage.design.dto.DesignStageOptionVO;
import com.storage.design.entity.DesignGuide;
import com.storage.design.entity.DesignProductType;
import com.storage.design.entity.DesignStage;
import com.storage.design.exception.DesignGuideNotFoundException;
import com.storage.design.excel.DesignGuideImportRow;
import com.storage.design.mapper.DesignGuideMapper;
import com.storage.design.query.DesignGuideQueryBuilder;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignGuideServiceImpl extends ServiceImpl<DesignGuideMapper, DesignGuide>
        implements DesignGuideService {

    private final DesignGuideConverter converter;
    private final DesignProductTypeService productTypeService;
    private final DesignStageService stageService;
    private final DesignGuideExportService exportService;
    private final OperatorResolver operatorResolver;

    @Override
    public PageResult<DesignGuide> page(DesignGuideQueryDTO query) {
        var result = page(PageSupport.page(query.getPage(), query.getPageSize()),
                DesignGuideQueryBuilder.build(query));
        return PageSupport.result(result);
    }

    @Override
    public DesignGuide getById(Long id) {
        DesignGuide entity = super.getById(id);
        if (entity == null) {
            throw new DesignGuideNotFoundException(id);
        }
        return entity;
    }

    @Override
    public DesignGuide create(DesignGuideSaveDTO dto) {
        DesignProductType productType = productTypeService.requireEnabled(dto.getProductTypeId());
        DesignStage stage = stageService.requireEnabled(dto.getStageId());
        assertNotDuplicate(dto, null);
        DesignGuide entity = converter.toNewEntity(dto);
        fillSnapshots(entity, productType, stage);
        fillRecorder(entity);
        save(entity);
        return entity;
    }

    @Override
    public DesignGuide update(Long id, DesignGuideSaveDTO dto) {
        DesignGuide existing = getById(id);
        DesignProductType productType = productTypeService.requireEnabled(dto.getProductTypeId());
        DesignStage stage = stageService.requireEnabled(dto.getStageId());
        assertNotDuplicate(dto, id);
        converter.applySaveDto(existing, dto);
        fillSnapshots(existing, productType, stage);
        fillRecorder(existing);
        updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        removeById(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    @Override
    public List<DesignGuide> listByQuery(DesignGuideQueryDTO query) {
        return list(DesignGuideQueryBuilder.build(query));
    }

    @Override
    public DesignGuideFilterOptionsVO filterOptions() {
        List<DesignProductTypeOptionVO> productTypes = productTypeService.listEnabled().stream()
                .map(type -> new DesignProductTypeOptionVO(type.getId(), type.getTypeCode(), type.getTypeName(),
                        type.getTypeCode() + " / " + type.getTypeName()))
                .toList();
        List<DesignStageOptionVO> stages = stageService.listEnabled().stream()
                .map(stage -> new DesignStageOptionVO(stage.getId(), stage.getSortOrder(), stage.getStageName(),
                        stage.getSortOrder() + ". " + stage.getStageName()))
                .toList();
        List<String> scopes = list().stream()
                .map(DesignGuide::getScope)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        return new DesignGuideFilterOptionsVO(productTypes, stages, scopes);
    }

    @Override
    public byte[] export(DesignGuideQueryDTO query) throws IOException {
        return exportService.export(listByQuery(query));
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return exportService.exportTemplate();
    }

    @Override
    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return AutoPoiExcelTemplate.importRows(file, DesignGuideImportRow.class, this::isEmptyRow, (excelRow, row) -> {
            DesignProductType productType = productTypeService.findEnabledByCodeOrName(
                    row.getProductTypeCode(), row.getProductTypeName());
            DesignStage stage = stageService.findEnabledByName(row.getStageName());
            DesignGuideSaveDTO dto = new DesignGuideSaveDTO();
            dto.setProductTypeId(productType.getId());
            dto.setStageId(stage.getId());
            dto.setScope(row.getScope());
            dto.setCheckItem(row.getCheckItem());
            dto.setRemark(row.getRemark());
            create(dto);
        });
    }

    private void assertNotDuplicate(DesignGuideSaveDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getScope()) || !StringUtils.hasText(dto.getCheckItem())) {
            throw new BusinessException("适用范围和检查项不能为空");
        }
        LambdaQueryWrapper<DesignGuide> wrapper = Wrappers.<DesignGuide>lambdaQuery()
                .eq(DesignGuide::getProductTypeId, dto.getProductTypeId())
                .eq(DesignGuide::getStageId, dto.getStageId())
                .eq(DesignGuide::getScope, dto.getScope().trim())
                .eq(DesignGuide::getCheckItem, dto.getCheckItem().trim());
        if (excludeId != null) {
            wrapper.ne(DesignGuide::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("相同产品类型/阶段/适用范围/检查项的设计指引已存在");
        }
    }

    private void fillSnapshots(DesignGuide entity, DesignProductType productType, DesignStage stage) {
        entity.setProductTypeId(productType.getId());
        entity.setProductTypeCode(productType.getTypeCode());
        entity.setProductTypeName(productType.getTypeName());
        entity.setStageId(stage.getId());
        entity.setStageName(stage.getStageName());
    }

    private void fillRecorder(DesignGuide entity) {
        OperatorInfo operator = operatorResolver.requireCurrentOperator();
        entity.setRecorderUserId(operator.getId());
        entity.setRecorderName(operator.getUsername());
        entity.setRecordedAt(LocalDateTime.now());
    }

    private boolean isEmptyRow(DesignGuideImportRow row) {
        return !StringUtils.hasText(row.getProductTypeCode())
                && !StringUtils.hasText(row.getProductTypeName())
                && !StringUtils.hasText(row.getStageName())
                && !StringUtils.hasText(row.getScope())
                && !StringUtils.hasText(row.getCheckItem())
                && !StringUtils.hasText(row.getRemark());
    }
}
