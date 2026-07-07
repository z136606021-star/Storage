package com.storage.warehouse.safety.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.safety.dto.SafetyStockRecordVO;
import com.storage.warehouse.safety.excel.SafetyStockExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SafetyStockExportService {

    public byte[] export(List<SafetyStockRecordVO> records) throws IOException {
        List<SafetyStockExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (SafetyStockRecordVO record : records) {
            SafetyStockExportRow row = new SafetyStockExportRow();
            row.setIndex(rowIndex++);
            row.setCategory(record.getCategory());
            row.setGenericName(record.getGenericName());
            row.setBrand(record.getBrand());
            row.setName(record.getName());
            row.setModel(record.getModel());
            row.setBinLocation(record.getBinLocation());
            row.setStockQuantity(record.getStockQuantity());
            row.setSafetyQuantity(record.getSafetyQuantity());
            row.setWarningPeriod(
                    SafetyStockWarningStatus.formatWarningPeriod(Boolean.TRUE.equals(record.getInWarningPeriod()))
            );
            rows.add(row);
        }

        return AutoPoiExcelTemplate.exportBytes("安全库存", SafetyStockExportRow.class, rows);
    }
}
