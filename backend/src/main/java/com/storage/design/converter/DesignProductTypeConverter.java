package com.storage.design.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.design.dto.DesignProductTypeSaveDTO;
import com.storage.design.entity.DesignProductType;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface DesignProductTypeConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "operatorUserId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operatedAt", ignore = true)
    @Mapping(target = "typeCode", source = "typeCode", qualifiedByName = "trim")
    @Mapping(target = "typeName", source = "typeName", qualifiedByName = "trim")
    @Mapping(target = "enabled", expression = "java(Boolean.FALSE.equals(dto.getEnabled()) ? 0 : 1)")
    DesignProductType toNewEntity(DesignProductTypeSaveDTO dto);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget DesignProductType entity, DesignProductTypeSaveDTO dto);
}
