package com.storage.warehouse.bom.converter;

import com.storage.warehouse.bom.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.bom.entity.WarehouseBom;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WarehouseBomConverter {

    public WarehouseBom toNewEntity(WarehouseBomSaveDTO dto) {
        WarehouseBom entity = new WarehouseBom();
        applySaveDto(entity, dto);
        return entity;
    }

    public void applySaveDto(WarehouseBom entity, WarehouseBomSaveDTO dto) {
        entity.setCategory(dto.getCategory().trim());
        entity.setGenericName(dto.getGenericName().trim());
        entity.setBrand(trimToNull(dto.getBrand()));
        entity.setName(dto.getName().trim());
        entity.setModel(dto.getModel().trim());
        entity.setRemark(trimToNull(dto.getRemark()));
        entity.setImageObjectKey(trimToNull(dto.getImageObjectKey()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
