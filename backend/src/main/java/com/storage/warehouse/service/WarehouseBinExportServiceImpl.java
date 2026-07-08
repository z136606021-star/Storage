package com.storage.warehouse.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.excel.WarehouseBinExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarehouseBinExportServiceImpl implements WarehouseBinExportService {

    public byte[] export(List<WarehouseBin> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<WarehouseBin> records) throws IOException {
        List<WarehouseBinExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (WarehouseBin record : records) {
            WarehouseBinExportRow row = new WarehouseBinExportRow();
            row.setIndex(rowIndex++);
            row.setBinCode(record.getBinCode());
            row.setRowNo(record.getRowNo());
            row.setColNo(record.getColNo());
            row.setLevelNo(record.getLevelNo());
            row.setRemark(record.getRemark());
            rows.add(row);
        }
        return AutoPoiExcelTemplate.exportBytes("Bin位", WarehouseBinExportRow.class, rows);
    }
}
