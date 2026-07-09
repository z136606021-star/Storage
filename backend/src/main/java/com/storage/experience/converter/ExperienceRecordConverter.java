package com.storage.experience.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.experience.dto.ExperienceRecordSaveDTO;
import com.storage.experience.entity.ExperienceRecord;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface ExperienceRecordConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recorderUserId", ignore = true)
    @Mapping(target = "recorderName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "description", source = "description", qualifiedByName = "trim")
    @Mapping(target = "impact", source = "impact", qualifiedByName = "trimToNull")
    @Mapping(target = "suggestion", source = "suggestion", qualifiedByName = "trimToNull")
    @Mapping(target = "actionPlan", source = "actionPlan", qualifiedByName = "trimToNull")
    ExperienceRecord toNewEntity(ExperienceRecordSaveDTO dto);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget ExperienceRecord entity, ExperienceRecordSaveDTO dto);
}
