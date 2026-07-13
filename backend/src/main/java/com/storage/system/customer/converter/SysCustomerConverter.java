package com.storage.system.customer.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.system.customer.dto.SysCustomerSaveDTO;
import com.storage.system.customer.entity.SysCustomer;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = StringMapping.class)
public interface SysCustomerConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "customerCode", source = "customerCode", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    @Mapping(target = "contactName", source = "contactName", qualifiedByName = "trimToNull")
    @Mapping(target = "phone", source = "phone", qualifiedByName = "trimToNull")
    @Mapping(target = "email", source = "email", qualifiedByName = "trimToNullLowercase")
    @Mapping(target = "address", source = "address", qualifiedByName = "trimToNull")
    @Mapping(target = "remark", source = "remark", qualifiedByName = "trimToNull")
    @Mapping(target = "status", source = "status", defaultValue = "1")
    SysCustomer toNewEntity(SysCustomerSaveDTO dto);

    @InheritConfiguration(name = "toNewEntity")
    void applySaveDto(@MappingTarget SysCustomer entity, SysCustomerSaveDTO dto);
}
