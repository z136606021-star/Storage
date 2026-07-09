package com.storage.design.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.design.dto.DesignGuideSaveDTO;
import com.storage.design.entity.DesignGuide;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface DesignGuideConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "productTypeCode", ignore = true)
    @Mapping(target = "productTypeName", ignore = true)
    @Mapping(target = "stageName", ignore = true)
    @Mapping(target = "recorderUserId", ignore = true)
    @Mapping(target = "recorderName", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    @Mapping(target = "scope", source = "scope", qualifiedByName = "trim")
    @Mapping(target = "checkItem", source = "checkItem", qualifiedByName = "trim")
    @Mapping(target = "remark", source = "remark", qualifiedByName = "trimToNull")
    DesignGuide toNewEntity(DesignGuideSaveDTO dto);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget DesignGuide entity, DesignGuideSaveDTO dto);
}
