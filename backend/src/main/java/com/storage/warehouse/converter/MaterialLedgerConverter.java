package com.storage.warehouse.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.warehouse.dto.MaterialSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface MaterialLedgerConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stockQuantity", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "trim")
    @Mapping(target = "genericName", source = "genericName", qualifiedByName = "trim")
    @Mapping(target = "brand", source = "brand", qualifiedByName = "trimToEmpty")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    @Mapping(target = "model", source = "model", qualifiedByName = "trim")
    @Mapping(target = "binLocation", source = "binLocation", qualifiedByName = "trim")
    @Mapping(target = "remark", source = "remark", qualifiedByName = "trimToNull")
    MaterialLedger toEntity(MaterialSaveDTO dto);

    @InheritConfiguration(name = "toEntity")
    @Mapping(target = "stockQuantity", constant = "0")
    MaterialLedger toNewEntity(MaterialSaveDTO dto);

    @InheritConfiguration(name = "toEntity")
    void applySaveDto(@MappingTarget MaterialLedger entity, MaterialSaveDTO dto);
}
