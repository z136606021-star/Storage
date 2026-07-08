package com.storage.warehouse.ledger.converter;

import com.storage.warehouse.ledger.dto.MaterialSaveDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MaterialLedgerConverter {

    public MaterialLedger toEntity(MaterialSaveDTO dto) {
        MaterialLedger entity = new MaterialLedger();
        applySaveDto(entity, dto);
        return entity;
    }

    public MaterialLedger toNewEntity(MaterialSaveDTO dto) {
        MaterialLedger entity = toEntity(dto);
        entity.setStockQuantity(0);
        return entity;
    }

    public void applySaveDto(MaterialLedger entity, MaterialSaveDTO dto) {
        entity.setCategory(dto.getCategory().trim());
        entity.setGenericName(dto.getGenericName().trim());
        entity.setBrand(trimToEmpty(dto.getBrand()));
        entity.setName(dto.getName().trim());
        entity.setModel(dto.getModel().trim());
        entity.setBinLocation(dto.getBinLocation().trim());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setRemark(trimToNull(dto.getRemark()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimToEmpty(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }
}
