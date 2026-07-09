package com.storage.warehouse.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.MaterialIoRecordVO;
import com.storage.warehouse.excel.MaterialIoExportRow;
import com.storage.warehouse.excel.MaterialIoImportTemplateRow;
import com.storage.warehouse.query.MaterialIoQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaterialIoExportServiceImpl implements MaterialIoExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(List<MaterialIoRecordVO> records) throws IOException {
        List<MaterialIoExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (MaterialIoRecordVO record : records) {
            MaterialIoExportRow row = new MaterialIoExportRow();
            row.setIndex(rowIndex++);
            row.setCategory(record.getCategory());
            row.setGenericName(record.getGenericName());
            row.setBrand(record.getBrand());
            row.setName(record.getName());
            row.setModel(record.getModel());
            row.setBinLocation(record.getBinLocation());
            row.setQuantity(record.getQuantity());
            row.setUnitPrice(record.getUnitPrice());
            row.setRemark(record.getRemark());
            row.setPurpose(record.getPurposeLabel());
            row.setProjectRef(record.getProjectRef());
            row.setIoType(MaterialIoQueryBuilder.ioTypeLabel(record.getIoType()));
            row.setOperator(formatOperator(record));
            row.setOperatedAt(record.getOperatedAt() == null ? "" : DATE_TIME_FORMATTER.format(record.getOperatedAt()));
            rows.add(row);
        }
        return AutoPoiExcelTemplate.exportBytes("物料出入库", MaterialIoExportRow.class, rows);
    }

    public byte[] exportTemplate() throws IOException {
        return exportImportTemplate();
    }

    public byte[] exportImportTemplate() throws IOException {
        return AutoPoiExcelTemplate.exportBytes("物料出入库导入", MaterialIoImportTemplateRow.class, List.of());
    }

    private String formatOperator(MaterialIoRecordVO record) {
        if (record.getOperatorDisplayName() != null && !record.getOperatorDisplayName().isBlank()) {
            return record.getOperatorDisplayName();
        }
        return record.getOperatorUsername() == null ? "" : record.getOperatorUsername();
    }
}
