package com.storage.design.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.design.dto.DesignStageSaveDTO;
import com.storage.design.entity.DesignStage;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface DesignStageConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "operatorUserId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operatedAt", ignore = true)
    @Mapping(target = "stageName", source = "stageName", qualifiedByName = "trim")
    @Mapping(target = "enabled", expression = "java(Boolean.FALSE.equals(dto.getEnabled()) ? 0 : 1)")
    DesignStage toNewEntity(DesignStageSaveDTO dto);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget DesignStage entity, DesignStageSaveDTO dto);
}
