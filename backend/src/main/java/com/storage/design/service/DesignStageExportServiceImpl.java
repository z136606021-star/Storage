package com.storage.design.service;

import com.storage.common.excel.ExcelExportWriter;
import com.storage.design.entity.DesignStage;
import com.storage.design.excel.DesignStageExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DesignStageExportServiceImpl implements DesignStageExportService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] export(List<DesignStage> records) throws IOException {
        List<DesignStageExportRow> rows = new ArrayList<>();
        for (DesignStage record : records) {
            DesignStageExportRow row = new DesignStageExportRow();
            row.setSortOrder(record.getSortOrder());
            row.setStageName(record.getStageName());
            row.setEnabled(formatEnabled(record.getEnabled()));
            row.setOperatorName(record.getOperatorName());
            row.setOperatedAt(record.getOperatedAt() == null ? "" : FORMATTER.format(record.getOperatedAt()));
            rows.add(row);
        }
        return ExcelExportWriter.writeBytes("阶段配置", DesignStageExportRow.class, rows);
    }

    private String formatEnabled(Integer enabled) {
        return enabled != null && enabled == 1 ? "启用" : "停用";
    }
}
