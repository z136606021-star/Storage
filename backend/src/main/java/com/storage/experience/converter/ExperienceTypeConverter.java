package com.storage.experience.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.experience.dto.ExperienceTypeSaveDTO;
import com.storage.experience.entity.ExperienceType;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface ExperienceTypeConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    @Mapping(target = "status", source = "status", defaultValue = "1")
    @Mapping(target = "sortOrder", source = "sortOrder", defaultValue = "0")
    ExperienceType toNewEntity(ExperienceTypeSaveDTO dto);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget ExperienceType entity, ExperienceTypeSaveDTO dto);
}
