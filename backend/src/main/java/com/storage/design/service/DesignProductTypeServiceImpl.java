package com.storage.design.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.common.query.PageSupport;
import com.storage.design.converter.DesignProductTypeConverter;
import com.storage.design.dto.DesignProductTypeQueryDTO;
import com.storage.design.dto.DesignProductTypeSaveDTO;
import com.storage.design.entity.DesignGuide;
import com.storage.design.entity.DesignProductType;
import com.storage.design.exception.DesignProductTypeNotFoundException;
import com.storage.design.mapper.DesignGuideMapper;
import com.storage.design.mapper.DesignProductTypeMapper;
import com.storage.design.query.DesignProductTypeQueryBuilder;
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
public class DesignProductTypeServiceImpl extends ServiceImpl<DesignProductTypeMapper, DesignProductType>
        implements DesignProductTypeService {

    private final DesignProductTypeConverter converter;
    private final DesignGuideMapper designGuideMapper;
    private final DesignProductTypeExportService exportService;
    private final OperatorResolver operatorResolver;

    @Override
    public PageResult<DesignProductType> page(DesignProductTypeQueryDTO query) {
        var result = page(PageSupport.page(query.getPage(), query.getPageSize()),
                DesignProductTypeQueryBuilder.build(query));
        return PageSupport.result(result);
    }

    @Override
    public DesignProductType getById(Long id) {
        DesignProductType entity = super.getById(id);
        if (entity == null) {
            throw new DesignProductTypeNotFoundException(id);
        }
        return entity;
    }

    @Override
    public DesignProductType create(DesignProductTypeSaveDTO dto) {
        assertTypeCodeUnique(dto.getTypeCode(), null);
        DesignProductType entity = converter.toNewEntity(dto);
        fillOperator(entity);
        save(entity);
        return entity;
    }

    @Override
    public DesignProductType update(Long id, DesignProductTypeSaveDTO dto) {
        DesignProductType existing = getById(id);
        assertTypeCodeUnique(dto.getTypeCode(), id);
        converter.applySaveDto(existing, dto);
        fillOperator(existing);
        updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        long usageCount = designGuideMapper.selectCount(Wrappers.<DesignGuide>lambdaQuery()
                .eq(DesignGuide::getProductTypeId, id));
        if (usageCount > 0) {
            throw new BusinessException("该产品类型已被 " + usageCount + " 条设计指引引用，无法删除");
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
    public List<DesignProductType> listByQuery(DesignProductTypeQueryDTO query) {
        return list(DesignProductTypeQueryBuilder.build(query));
    }

    @Override
    public List<DesignProductType> listEnabled() {
        DesignProductTypeQueryDTO query = new DesignProductTypeQueryDTO();
        query.setEnabled(1);
        return listByQuery(query);
    }

    @Override
    public DesignProductType requireEnabled(Long id) {
        DesignProductType entity = getById(id);
        if (entity.getEnabled() == null || entity.getEnabled() != 1) {
            throw new BusinessException("产品类型已停用，不能用于新增或编辑设计指引");
        }
        return entity;
    }

    @Override
    public DesignProductType findEnabledByCodeOrName(String typeCode, String typeName) {
        LambdaQueryWrapper<DesignProductType> wrapper = Wrappers.<DesignProductType>lambdaQuery()
                .eq(DesignProductType::getEnabled, 1);
        if (StringUtils.hasText(typeCode)) {
            wrapper.eq(DesignProductType::getTypeCode, typeCode.trim());
        } else if (StringUtils.hasText(typeName)) {
            wrapper.eq(DesignProductType::getTypeName, typeName.trim());
        } else {
            throw new BusinessException("产品类型编号或产品类型不能为空");
        }
        DesignProductType entity = getOne(wrapper, false);
        if (entity == null) {
            throw new BusinessException("未找到启用的产品类型: " + (StringUtils.hasText(typeCode) ? typeCode : typeName));
        }
        return entity;
    }

    @Override
    public byte[] export(DesignProductTypeQueryDTO query) throws IOException {
        return exportService.export(listByQuery(query));
    }

    private void assertTypeCodeUnique(String typeCode, Long excludeId) {
        LambdaQueryWrapper<DesignProductType> wrapper = Wrappers.<DesignProductType>lambdaQuery()
                .eq(DesignProductType::getTypeCode, typeCode.trim());
        if (excludeId != null) {
            wrapper.ne(DesignProductType::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("产品类型编号已存在: " + typeCode);
        }
    }

    private void fillOperator(DesignProductType entity) {
        OperatorInfo operator = operatorResolver.requireCurrentOperator();
        entity.setOperatorUserId(operator.getId());
        entity.setOperatorName(operator.getUsername());
        entity.setOperatedAt(LocalDateTime.now());
    }
}
