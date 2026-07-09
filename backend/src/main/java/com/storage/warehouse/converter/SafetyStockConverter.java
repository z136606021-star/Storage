package com.storage.warehouse.converter;

import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.dto.SafetyStockRecordVO;
import com.storage.warehouse.entity.SafetyStock;
import com.storage.warehouse.service.SafetyStockWarningStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SafetyStockConverter {

    default void enrichWarningPeriod(SafetyStockRecordVO vo) {
        if (vo == null) {
            return;
        }
        vo.setInWarningPeriod(SafetyStockWarningStatus.inWarning(
                vo.getStockQuantity(),
                vo.getSafetyQuantity(),
                vo.getWarningEnabled()
        ));
    }

    @Mapping(target = "materialLedgerId", source = "ledger.id")
    @Mapping(target = "category", source = "ledger.category")
    @Mapping(target = "genericName", source = "ledger.genericName")
    @Mapping(target = "brand", source = "ledger.brand")
    @Mapping(target = "name", source = "ledger.name")
    @Mapping(target = "model", source = "ledger.model")
    @Mapping(target = "binLocation", source = "ledger.binLocation")
    @Mapping(target = "stockQuantity", source = "ledger.stockQuantity")
    @Mapping(target = "safetyStockId", source = "safetyStock.id")
    @Mapping(target = "safetyQuantity", source = "safetyStock.safetyQuantity")
    @Mapping(target = "warningEnabled", source = "safetyStock.warningEnabled")
    @Mapping(target = "createdAt", source = "safetyStock.createdAt")
    @Mapping(target = "updatedAt", source = "safetyStock.updatedAt")
    @Mapping(target = "inWarningPeriod", ignore = true)
    SafetyStockRecordVO toVo(MaterialLedger ledger, SafetyStock safetyStock);

    @AfterMapping
    default void fillDefaultsAndWarningStatus(SafetyStock safetyStock, @MappingTarget SafetyStockRecordVO vo) {
        if (safetyStock == null) {
            vo.setSafetyQuantity(0);
            vo.setWarningEnabled(false);
        } else {
            vo.setWarningEnabled(SafetyStockWarningStatus.isAutoWarningEnabled(vo.getSafetyQuantity()));
        }
        enrichWarningPeriod(vo);
    }
}
