package com.storage.warehouse.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.entity.WarehouseBom;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface WarehouseBomConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "trim")
    @Mapping(target = "genericName", source = "genericName", qualifiedByName = "trim")
    @Mapping(target = "brand", source = "brand", qualifiedByName = "trimToNull")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    @Mapping(target = "model", source = "model", qualifiedByName = "trim")
    @Mapping(target = "remark", source = "remark", qualifiedByName = "trimToNull")
    @Mapping(target = "imageObjectKey", source = "imageObjectKey", qualifiedByName = "trimToNull")
    WarehouseBom toNewEntity(WarehouseBomSaveDTO dto);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget WarehouseBom entity, WarehouseBomSaveDTO dto);
}
