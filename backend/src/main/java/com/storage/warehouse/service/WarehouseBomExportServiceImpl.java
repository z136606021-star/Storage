package com.storage.warehouse.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.excel.WarehouseBomExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarehouseBomExportServiceImpl implements WarehouseBomExportService {

    public byte[] export(List<WarehouseBom> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<WarehouseBom> records) throws IOException {
        List<WarehouseBomExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (WarehouseBom record : records) {
            WarehouseBomExportRow row = new WarehouseBomExportRow();
            row.setIndex(rowIndex++);
            row.setCategory(record.getCategory());
            row.setGenericName(record.getGenericName());
            row.setBrand(record.getBrand());
            row.setName(record.getName());
            row.setModel(record.getModel());
            row.setRemark(record.getRemark());
            rows.add(row);
        }
        return AutoPoiExcelTemplate.exportBytes("物料清单", WarehouseBomExportRow.class, rows);
    }
}
