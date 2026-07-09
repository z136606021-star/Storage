package com.storage.design.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.common.query.PageSupport;
import com.storage.design.converter.DesignStageConverter;
import com.storage.design.dto.DesignStageQueryDTO;
import com.storage.design.dto.DesignStageSaveDTO;
import com.storage.design.entity.DesignGuide;
import com.storage.design.entity.DesignStage;
import com.storage.design.exception.DesignStageNotFoundException;
import com.storage.design.mapper.DesignGuideMapper;
import com.storage.design.mapper.DesignStageMapper;
import com.storage.design.query.DesignStageQueryBuilder;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignStageServiceImpl extends ServiceImpl<DesignStageMapper, DesignStage>
        implements DesignStageService {

    private final DesignStageConverter converter;
    private final DesignGuideMapper designGuideMapper;
    private final DesignStageExportService exportService;
    private final OperatorResolver operatorResolver;

    @Override
    public PageResult<DesignStage> page(DesignStageQueryDTO query) {
        var result = page(PageSupport.page(query.getPage(), query.getPageSize()),
                DesignStageQueryBuilder.build(query));
        return PageSupport.result(result);
    }

    @Override
    public DesignStage getById(Long id) {
        DesignStage entity = super.getById(id);
        if (entity == null) {
            throw new DesignStageNotFoundException(id);
        }
        return entity;
    }

    @Override
    public DesignStage create(DesignStageSaveDTO dto) {
        assertSortOrderUnique(dto.getSortOrder(), null);
        DesignStage entity = converter.toNewEntity(dto);
        fillOperator(entity);
        save(entity);
        return entity;
    }

    @Override
    public DesignStage update(Long id, DesignStageSaveDTO dto) {
        DesignStage existing = getById(id);
        assertSortOrderUnique(dto.getSortOrder(), id);
        converter.applySaveDto(existing, dto);
        fillOperator(existing);
        updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        long usageCount = designGuideMapper.selectCount(Wrappers.<DesignGuide>lambdaQuery()
                .eq(DesignGuide::getStageId, id));
        if (usageCount > 0) {
            throw new BusinessException("该项目阶段已被 " + usageCount + " 条设计指引引用，无法删除");
        }
        removeById(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    @Override
    public List<DesignStage> listByQuery(DesignStageQueryDTO query) {
        return list(DesignStageQueryBuilder.build(query));
    }

    @Override
    public List<DesignStage> listEnabled() {
        DesignStageQueryDTO query = new DesignStageQueryDTO();
        query.setEnabled(1);
        return listByQuery(query);
    }

    @Override
    public DesignStage requireEnabled(Long id) {
        DesignStage entity = getById(id);
        if (entity.getEnabled() == null || entity.getEnabled() != 1) {
            throw new BusinessException("项目阶段已停用，不能用于新增或编辑设计指引");
        }
        return entity;
    }

    @Override
    public DesignStage findEnabledByName(String stageName) {
        if (!StringUtils.hasText(stageName)) {
            throw new BusinessException("项目阶段不能为空");
        }
        DesignStage entity = getOne(Wrappers.<DesignStage>lambdaQuery()
                .eq(DesignStage::getEnabled, 1)
                .eq(DesignStage::getStageName, stageName.trim()), false);
        if (entity == null) {
            throw new BusinessException("未找到启用的项目阶段: " + stageName);
        }
        return entity;
    }

    @Override
    public byte[] export(DesignStageQueryDTO query) throws IOException {
        return exportService.export(listByQuery(query));
    }

    private void assertSortOrderUnique(Integer sortOrder, Long excludeId) {
        LambdaQueryWrapper<DesignStage> wrapper = Wrappers.<DesignStage>lambdaQuery()
                .eq(DesignStage::getSortOrder, sortOrder);
        if (excludeId != null) {
            wrapper.ne(DesignStage::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("阶段顺序已存在: " + sortOrder);
        }
    }

    private void fillOperator(DesignStage entity) {
        OperatorInfo operator = operatorResolver.requireCurrentOperator();
        entity.setOperatorUserId(operator.getId());
        entity.setOperatorName(operator.getUsername());
        entity.setOperatedAt(LocalDateTime.now());
    }
}
