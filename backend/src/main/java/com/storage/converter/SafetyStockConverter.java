package com.storage.converter;

import com.storage.dto.SafetyStockRecordVO;
import com.storage.entity.MaterialLedger;
import com.storage.entity.SafetyStock;
import com.storage.service.SafetyStockWarningStatus;
import org.springframework.stereotype.Component;

@Component
public class SafetyStockConverter {

    public void enrichWarningPeriod(SafetyStockRecordVO vo) {
        if (vo == null) {
            return;
        }
        vo.setInWarningPeriod(SafetyStockWarningStatus.inWarning(
                vo.getStockQuantity(),
                vo.getSafetyQuantity(),
                vo.getWarningEnabled()
        ));
    }

    public SafetyStockRecordVO toVo(MaterialLedger ledger, SafetyStock safetyStock) {
        SafetyStockRecordVO vo = new SafetyStockRecordVO();
        vo.setMaterialLedgerId(ledger.getId());
        vo.setCategory(ledger.getCategory());
        vo.setGenericName(ledger.getGenericName());
        vo.setBrand(ledger.getBrand());
        vo.setName(ledger.getName());
        vo.setModel(ledger.getModel());
        vo.setBinLocation(ledger.getBinLocation());
        vo.setStockQuantity(ledger.getStockQuantity());

        if (safetyStock != null) {
            vo.setSafetyStockId(safetyStock.getId());
            vo.setSafetyQuantity(safetyStock.getSafetyQuantity());
            vo.setWarningEnabled(safetyStock.getWarningEnabled());
            vo.setCreatedAt(safetyStock.getCreatedAt());
            vo.setUpdatedAt(safetyStock.getUpdatedAt());
        } else {
            vo.setSafetyQuantity(0);
            vo.setWarningEnabled(false);
        }

        enrichWarningPeriod(vo);
        return vo;
    }
}
