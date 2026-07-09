package com.storage.experience.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.experience.dto.ExperienceRecordSaveDTO;
import com.storage.experience.entity.ExperienceType;
import com.storage.experience.excel.ExperienceExportRow;
import com.storage.experience.mapper.ExperienceTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperienceRecordImportServiceImpl implements ExperienceRecordImportService {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private final ExperienceRecordService experienceRecordService;
    private final ExperienceTypeMapper experienceTypeMapper;

    @Override
    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return AutoPoiExcelTemplate.importRows(file, ExperienceExportRow.class, this::isEmptyRow, (excelRow, row) ->
                experienceRecordService.createImported(parseRow(row), row.getRecorderName()));
    }

    private ExperienceRecordSaveDTO parseRow(ExperienceExportRow row) {
        if (!StringUtils.hasText(row.getTypeName())) {
            throw new IllegalArgumentException("类型不能为空");
        }
        if (!StringUtils.hasText(row.getDescription())) {
            throw new IllegalArgumentException("描述不能为空");
        }
        ExperienceType type = experienceTypeMapper.selectOne(Wrappers.<ExperienceType>lambdaQuery()
                .eq(ExperienceType::getName, row.getTypeName().trim())
                .last("LIMIT 1"));
        if (type == null) {
            throw new IllegalArgumentException("类型不存在: " + row.getTypeName());
        }

        ExperienceRecordSaveDTO dto = new ExperienceRecordSaveDTO();
        dto.setTypeId(type.getId());
        dto.setDescription(row.getDescription());
        dto.setImpact(row.getImpact());
        dto.setSuggestion(row.getSuggestion());
        dto.setActionPlan(row.getActionPlan());
        dto.setProjectNames(parseProjects(row.getProjects()));
        dto.setRecordedAt(parseRecordedAt(row.getRecordedAt()));
        return dto;
    }

    private List<String> parseProjects(String projects) {
        if (!StringUtils.hasText(projects)) {
            return List.of();
        }
        return Arrays.stream(projects.split("[,，;；\\n]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private LocalDateTime parseRecordedAt(String recordedAt) {
        if (!StringUtils.hasText(recordedAt)) {
            return null;
        }
        String value = recordedAt.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next supported format.
            }
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("记录时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private boolean isEmptyRow(ExperienceExportRow row) {
        return !StringUtils.hasText(row.getTypeName())
                && !StringUtils.hasText(row.getDescription())
                && !StringUtils.hasText(row.getImpact())
                && !StringUtils.hasText(row.getSuggestion())
                && !StringUtils.hasText(row.getActionPlan())
                && !StringUtils.hasText(row.getProjects())
                && !StringUtils.hasText(row.getRecorderName())
                && !StringUtils.hasText(row.getRecordedAt());
    }
}
