package com.storage.design.service;

import com.storage.common.excel.ExcelExportWriter;
import com.storage.design.entity.DesignGuide;
import com.storage.design.excel.DesignGuideExportRow;
import com.storage.design.excel.DesignGuideImportTemplateRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DesignGuideExportServiceImpl implements DesignGuideExportService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] export(List<DesignGuide> records) throws IOException {
        List<DesignGuideExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (DesignGuide record : records) {
            DesignGuideExportRow row = new DesignGuideExportRow();
            row.setIndex(rowIndex++);
            row.setProductTypeCode(record.getProductTypeCode());
            row.setProductTypeName(record.getProductTypeName());
            row.setStageName(record.getStageName());
            row.setScope(record.getScope());
            row.setCheckItem(record.getCheckItem());
            row.setRemark(record.getRemark());
            row.setRecorderName(record.getRecorderName());
            row.setRecordedAt(record.getRecordedAt() == null ? "" : FORMATTER.format(record.getRecordedAt()));
            rows.add(row);
        }
        return ExcelExportWriter.writeBytes("设计指引", DesignGuideExportRow.class, rows);
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return ExcelExportWriter.writeBytes("设计指引导入", DesignGuideImportTemplateRow.class, List.of());
    }
}
