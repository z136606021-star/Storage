package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.excel.WarehouseBinImportTemplateRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class WarehouseBinImportServiceImpl implements WarehouseBinImportService {

    private final WarehouseBinService warehouseBinService;

    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return AutoPoiExcelTemplate.importRows(
                file,
                WarehouseBinImportTemplateRow.class,
                this::isEmptyRow,
                (excelRow, row) -> warehouseBinService.create(parseRow(row))
        );
    }

    private WarehouseBinSaveDTO parseRow(WarehouseBinImportTemplateRow row) {
        WarehouseBinSaveDTO dto = new WarehouseBinSaveDTO();
        dto.setRowNo(parseRequiredRow(row.getRowNo()));
        dto.setColNo(parsePositiveInt(row.getColNo(), "列", false));
        dto.setLevelNo(parsePositiveInt(row.getLevelNo(), "层", false));
        dto.setRemark(row.getRemark());
        return dto;
    }

    private String parseRequiredRow(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("排不能为空");
        }
        String normalized = value.trim();
        if (normalized.length() > 32) {
            throw new IllegalArgumentException("排长度不能超过32");
        }
        return normalized;
    }

    private Integer parsePositiveInt(Integer value, String label, boolean required) {
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException(label + "不能为空");
            }
            return null;
        }
        if (value < 1) {
            throw new IllegalArgumentException(label + "必须为正整数");
        }
        return value;
    }

    private boolean isEmptyRow(WarehouseBinImportTemplateRow row) {
        return row.getRowNo() == null
                && row.getColNo() == null
                && row.getLevelNo() == null
                && !StringUtils.hasText(row.getRemark());
    }
}
