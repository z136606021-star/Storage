package com.storage.warehouse.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.SafetyStockRecordVO;
import com.storage.warehouse.excel.SafetyStockExportRow;
import com.storage.warehouse.excel.SafetyStockPurchaseListExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SafetyStockExportServiceImpl implements SafetyStockExportService {

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

    @Override
    public byte[] exportPurchaseList(List<SafetyStockRecordVO> records) throws IOException {
        List<SafetyStockPurchaseListExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (SafetyStockRecordVO record : records) {
            Integer suggestedQuantity = suggestedPurchaseQuantity(record);
            if (suggestedQuantity == null || suggestedQuantity <= 0) {
                continue;
            }
            SafetyStockPurchaseListExportRow row = new SafetyStockPurchaseListExportRow();
            row.setIndex(rowIndex++);
            row.setCategory(record.getCategory());
            row.setGenericName(record.getGenericName());
            row.setBrand(record.getBrand());
            row.setName(record.getName());
            row.setModel(record.getModel());
            row.setBinLocation(record.getBinLocation());
            row.setStockQuantity(record.getStockQuantity());
            row.setSafetyQuantity(record.getSafetyQuantity());
            row.setSuggestedPurchaseQuantity(suggestedQuantity);
            row.setRemark("库存低于安全库存，请采购补足");
            rows.add(row);
        }

        return AutoPoiExcelTemplate.exportBytes("采购清单", SafetyStockPurchaseListExportRow.class, rows);
    }

    private Integer suggestedPurchaseQuantity(SafetyStockRecordVO record) {
        if (!Boolean.TRUE.equals(record.getInWarningPeriod())) {
            return null;
        }
        int stockQuantity = record.getStockQuantity() == null ? 0 : record.getStockQuantity();
        int safetyQuantity = record.getSafetyQuantity() == null ? 0 : record.getSafetyQuantity();
        return safetyQuantity - stockQuantity;
    }
}
