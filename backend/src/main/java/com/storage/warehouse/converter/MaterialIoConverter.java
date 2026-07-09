package com.storage.warehouse.converter;

import com.storage.common.mapper.StringMapping;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.warehouse.dto.MaterialIoBatchItemDTO;
import com.storage.warehouse.dto.MaterialIoRecordVO;
import com.storage.warehouse.dto.MaterialIoSaveDTO;
import com.storage.warehouse.dto.MaterialIoUpdateDTO;
import com.storage.warehouse.entity.MaterialIoRecord;
import com.storage.warehouse.service.MaterialIoPurpose;
import com.storage.warehouse.entity.MaterialLedger;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring", uses = StringMapping.class, imports = MaterialIoPurpose.class)
public interface MaterialIoConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "remark", source = "remark", qualifiedByName = "trimToNull")
    @Mapping(target = "purpose", source = "purpose", qualifiedByName = "trimToNull")
    @Mapping(target = "projectRef", source = "projectRef", qualifiedByName = "trimToNull")
    MaterialIoRecord toNewEntity(
            Long materialLedgerId,
            String ioType,
            Integer quantity,
            BigDecimal unitPrice,
            String remark,
            String purpose,
            String projectRef,
            Long operatorUserId,
            LocalDateTime operatedAt
    );

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "remark", source = "remark", qualifiedByName = "trimToNull")
    @Mapping(target = "purpose", source = "purpose", qualifiedByName = "trimToNull")
    @Mapping(target = "projectRef", source = "projectRef", qualifiedByName = "trimToNull")
    void applyUpdateDto(@MappingTarget MaterialIoRecord entity, MaterialIoUpdateDTO dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "materialLedgerId", source = "materialLedgerId")
    @Mapping(target = "ioType", source = "ioType")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "remark", source = "remark", qualifiedByName = "trimToNull")
    @Mapping(target = "purpose", source = "purpose", qualifiedByName = "trimToNull")
    @Mapping(target = "projectRef", source = "projectRef", qualifiedByName = "trimToNull")
    void applySaveDto(@MappingTarget MaterialIoRecord entity, MaterialIoSaveDTO dto);

    @Mapping(target = "id", source = "record.id")
    @Mapping(target = "materialLedgerId", source = "record.materialLedgerId")
    @Mapping(target = "ioType", source = "record.ioType")
    @Mapping(target = "quantity", source = "record.quantity")
    @Mapping(target = "unitPrice", source = "record.unitPrice")
    @Mapping(target = "remark", source = "record.remark")
    @Mapping(target = "purpose", source = "record.purpose")
    @Mapping(target = "purposeLabel", expression = "java(MaterialIoPurpose.purposeLabel(record.getPurpose()))")
    @Mapping(target = "projectRef", source = "record.projectRef")
    @Mapping(target = "operatorUserId", source = "record.operatorUserId")
    @Mapping(target = "operatorUsername", source = "operator.username")
    @Mapping(target = "operatorDisplayName", ignore = true)
    @Mapping(target = "operatedAt", source = "record.operatedAt")
    @Mapping(target = "createdAt", source = "record.createdAt")
    @Mapping(target = "updatedAt", source = "record.updatedAt")
    @Mapping(target = "category", source = "ledger.category")
    @Mapping(target = "genericName", source = "ledger.genericName")
    @Mapping(target = "brand", source = "ledger.brand")
    @Mapping(target = "name", source = "ledger.name")
    @Mapping(target = "model", source = "ledger.model")
    @Mapping(target = "binLocation", source = "ledger.binLocation")
    @Mapping(target = "stockQuantity", source = "ledger.stockQuantity")
    MaterialIoRecordVO toVo(
            MaterialIoRecord record,
            MaterialLedger ledger,
            OperatorInfo operator
    );

    @Mapping(target = "ioType", source = "ioType")
    @Mapping(target = "operatedAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "genericName", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "model", ignore = true)
    @Mapping(target = "binLocation", ignore = true)
    MaterialIoSaveDTO toSaveDto(MaterialIoBatchItemDTO item, String ioType);
}
