package com.storage.warehouse.ledger.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.excel.MaterialLedgerExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaterialLedgerExportService {

    public byte[] export(List<MaterialLedger> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<MaterialLedger> records) throws IOException {
        List<MaterialLedgerExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (MaterialLedger record : records) {
            MaterialLedgerExportRow row = new MaterialLedgerExportRow();
            row.setIndex(rowIndex++);
            row.setCategory(record.getCategory());
            row.setGenericName(record.getGenericName());
            row.setBrand(record.getBrand());
            row.setName(record.getName());
            row.setModel(record.getModel());
            row.setBinLocation(record.getBinLocation());
            row.setStockQuantity(record.getStockQuantity());
            row.setUnitPrice(formatUnitPrice(record.getUnitPrice()));
            row.setRemark(record.getRemark());
            rows.add(row);
        }
        return AutoPoiExcelTemplate.exportBytes("物料台账", MaterialLedgerExportRow.class, rows);
    }

    private String formatUnitPrice(BigDecimal unitPrice) {
        return unitPrice == null ? "" : unitPrice.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
