package com.storage.warehouse.bin.converter;

import com.storage.warehouse.bin.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.bin.entity.WarehouseBin;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WarehouseBinConverter {

    public void applySaveDto(WarehouseBin entity, WarehouseBinSaveDTO dto, String binCode) {
        entity.setBinCode(binCode);
        entity.setRowNo(dto.getRowNo());
        entity.setColNo(dto.getColNo());
        entity.setLevelNo(dto.getLevelNo());
        entity.setRemark(trimToNull(dto.getRemark()));
    }

    public WarehouseBin toNewEntity(WarehouseBinSaveDTO dto, String binCode) {
        WarehouseBin entity = new WarehouseBin();
        applySaveDto(entity, dto, binCode);
        return entity;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
