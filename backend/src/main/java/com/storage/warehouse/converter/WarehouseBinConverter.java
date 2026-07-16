package com.storage.warehouse.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.warehouse.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.entity.WarehouseBin;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface WarehouseBinConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "operatorUserId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "binCode", source = "binCode")
    @Mapping(target = "rowNo", source = "dto.rowNo", qualifiedByName = "trim")
    @Mapping(target = "remark", source = "dto.remark", qualifiedByName = "trimToNull")
    WarehouseBin toNewEntity(WarehouseBinSaveDTO dto, String binCode);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget WarehouseBin entity, WarehouseBinSaveDTO dto, String binCode);
}
