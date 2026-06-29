package com.storage.converter;

import com.storage.dto.MaterialIoBatchItemDTO;
import com.storage.dto.MaterialIoRecordVO;
import com.storage.dto.MaterialIoSaveDTO;
import com.storage.dto.MaterialIoUpdateDTO;
import com.storage.entity.MaterialIoRecord;
import com.storage.entity.MaterialLedger;
import com.storage.entity.SysUser;
import com.storage.service.MaterialIoPurpose;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MaterialIoConverter {

    public MaterialIoRecord toNewEntity(
            Long materialLedgerId,
            String ioType,
            Integer quantity,
            String remark,
            String purpose,
            String projectRef,
            Long operatorUserId,
            LocalDateTime operatedAt
    ) {
        MaterialIoRecord entity = new MaterialIoRecord();
        entity.setMaterialLedgerId(materialLedgerId);
        entity.setIoType(ioType);
        entity.setQuantity(quantity);
        entity.setRemark(trimToNull(remark));
        entity.setPurpose(trimToNull(purpose));
        entity.setProjectRef(trimToNull(projectRef));
        entity.setOperatorUserId(operatorUserId);
        entity.setOperatedAt(operatedAt);
        return entity;
    }

    public void applyUpdateDto(MaterialIoRecord entity, MaterialIoUpdateDTO dto) {
        entity.setQuantity(dto.getQuantity());
        entity.setRemark(trimToNull(dto.getRemark()));
        entity.setPurpose(trimToNull(dto.getPurpose()));
        entity.setProjectRef(trimToNull(dto.getProjectRef()));
    }

    public void applySaveDto(MaterialIoRecord entity, MaterialIoSaveDTO dto) {
        entity.setMaterialLedgerId(dto.getMaterialLedgerId());
        entity.setIoType(dto.getIoType());
        entity.setQuantity(dto.getQuantity());
        entity.setRemark(trimToNull(dto.getRemark()));
        entity.setPurpose(trimToNull(dto.getPurpose()));
        entity.setProjectRef(trimToNull(dto.getProjectRef()));
    }

    public MaterialIoRecordVO toVo(
            MaterialIoRecord record,
            MaterialLedger ledger,
            SysUser operator
    ) {
        MaterialIoRecordVO vo = new MaterialIoRecordVO();
        vo.setId(record.getId());
        vo.setMaterialLedgerId(record.getMaterialLedgerId());
        vo.setIoType(record.getIoType());
        vo.setQuantity(record.getQuantity());
        vo.setRemark(record.getRemark());
        vo.setPurpose(record.getPurpose());
        vo.setPurposeLabel(MaterialIoPurpose.purposeLabel(record.getPurpose()));
        vo.setProjectRef(record.getProjectRef());
        vo.setOperatorUserId(record.getOperatorUserId());
        if (operator != null) {
            vo.setOperatorUsername(operator.getUsername());
            vo.setOperatorDisplayName(operator.getDisplayName());
        }
        vo.setOperatedAt(record.getOperatedAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());

        if (ledger != null) {
            vo.setCategory(ledger.getCategory());
            vo.setGenericName(ledger.getGenericName());
            vo.setBrand(ledger.getBrand());
            vo.setName(ledger.getName());
            vo.setModel(ledger.getModel());
            vo.setBinLocation(ledger.getBinLocation());
            vo.setStockQuantity(ledger.getStockQuantity());
        }
        return vo;
    }

    public MaterialIoSaveDTO toSaveDto(MaterialIoBatchItemDTO item, String ioType) {
        MaterialIoSaveDTO dto = new MaterialIoSaveDTO();
        dto.setMaterialLedgerId(item.getMaterialLedgerId());
        dto.setIoType(ioType);
        dto.setQuantity(item.getQuantity());
        dto.setRemark(item.getRemark());
        dto.setPurpose(item.getPurpose());
        dto.setProjectRef(item.getProjectRef());
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
