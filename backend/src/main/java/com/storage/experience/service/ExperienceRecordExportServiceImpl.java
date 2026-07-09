package com.storage.experience.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.experience.dto.ExperienceRecordDetailVO;
import com.storage.experience.excel.ExperienceExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExperienceRecordExportServiceImpl implements ExperienceRecordExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] export(List<ExperienceRecordDetailVO> records) throws IOException {
        return exportWorkbook(records);
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<ExperienceRecordDetailVO> records) throws IOException {
        List<ExperienceExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (ExperienceRecordDetailVO record : records) {
            ExperienceExportRow row = new ExperienceExportRow();
            row.setIndex(rowIndex++);
            row.setTypeName(record.getTypeName());
            row.setDescription(record.getDescription());
            row.setImpact(record.getImpact());
            row.setSuggestion(record.getSuggestion());
            row.setActionPlan(record.getActionPlan());
            row.setProjects(String.join("；", record.getProjectNames()));
            row.setRecorderName(record.getRecorderName());
            row.setRecordedAt(record.getRecordedAt() == null ? null : DATE_TIME_FORMATTER.format(record.getRecordedAt()));
            rows.add(row);
        }
        return AutoPoiExcelTemplate.exportBytes("经验库", ExperienceExportRow.class, rows);
    }
}
