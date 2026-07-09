package com.storage.experience.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.exception.BusinessException;
import com.storage.experience.converter.ExperienceTypeConverter;
import com.storage.experience.dto.ExperienceTypeSaveDTO;
import com.storage.experience.entity.ExperienceRecord;
import com.storage.experience.entity.ExperienceType;
import com.storage.experience.exception.ExperienceTypeNotFoundException;
import com.storage.experience.mapper.ExperienceRecordMapper;
import com.storage.experience.mapper.ExperienceTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperienceTypeServiceImpl implements ExperienceTypeService {

    private final ExperienceTypeMapper experienceTypeMapper;
    private final ExperienceRecordMapper experienceRecordMapper;
    private final ExperienceTypeConverter experienceTypeConverter;

    @Override
    public List<ExperienceType> listAll() {
        return experienceTypeMapper.selectList(Wrappers.<ExperienceType>lambdaQuery()
                .orderByAsc(ExperienceType::getSortOrder)
                .orderByAsc(ExperienceType::getId));
    }

    @Override
    public List<ExperienceType> listEnabled() {
        return experienceTypeMapper.selectList(Wrappers.<ExperienceType>lambdaQuery()
                .eq(ExperienceType::getStatus, 1)
                .orderByAsc(ExperienceType::getSortOrder)
                .orderByAsc(ExperienceType::getId));
    }

    @Override
    public ExperienceType create(ExperienceTypeSaveDTO dto) {
        normalize(dto);
        assertNameUnique(dto.getName(), null);
        ExperienceType entity = experienceTypeConverter.toNewEntity(dto);
        experienceTypeMapper.insert(entity);
        return entity;
    }

    @Override
    public ExperienceType update(Long id, ExperienceTypeSaveDTO dto) {
        ExperienceType existing = getRequired(id);
        normalize(dto);
        assertNameUnique(dto.getName(), id);
        experienceTypeConverter.applySaveDto(existing, dto);
        experienceTypeMapper.updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        ExperienceType existing = getRequired(id);
        Long used = experienceRecordMapper.selectCount(Wrappers.<ExperienceRecord>lambdaQuery()
                .eq(ExperienceRecord::getTypeId, existing.getId()));
        if (used > 0) {
            throw new BusinessException("该类型已被经验记录引用，不能删除");
        }
        experienceTypeMapper.deleteById(id);
    }

    private ExperienceType getRequired(Long id) {
        ExperienceType type = experienceTypeMapper.selectById(id);
        if (type == null) {
            throw new ExperienceTypeNotFoundException(id);
        }
        return type;
    }

    private void normalize(ExperienceTypeSaveDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("类型名称不能为空");
        }
        dto.setName(dto.getName().trim());
        if (dto.getStatus() == null) {
            dto.setStatus(1);
        }
        if (dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BusinessException("类型状态无效");
        }
        if (dto.getSortOrder() == null) {
            dto.setSortOrder(0);
        }
    }

    private void assertNameUnique(String name, Long excludeId) {
        var wrapper = Wrappers.<ExperienceType>lambdaQuery().eq(ExperienceType::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(ExperienceType::getId, excludeId);
        }
        if (experienceTypeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("类型名称已存在: " + name);
        }
    }
}
