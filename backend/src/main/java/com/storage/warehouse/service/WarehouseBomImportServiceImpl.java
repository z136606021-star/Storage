package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.excel.WarehouseBomImportTemplateRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class WarehouseBomImportServiceImpl implements WarehouseBomImportService {

    private final WarehouseBomService warehouseBomService;

    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return AutoPoiExcelTemplate.importRows(file, WarehouseBomImportTemplateRow.class, this::isEmptyRow, (excelRow, row) -> {
            WarehouseBomSaveDTO dto = parseRow(row);
            validateDto(dto);
            warehouseBomService.create(dto);
        });
    }

    private WarehouseBomSaveDTO parseRow(WarehouseBomImportTemplateRow row) {
        WarehouseBomSaveDTO dto = new WarehouseBomSaveDTO();
        dto.setCategory(row.getCategory());
        dto.setGenericName(row.getGenericName());
        dto.setBrand(row.getBrand());
        dto.setName(row.getName());
        dto.setRemark(row.getRemark());
        return dto;
    }

    private void validateDto(WarehouseBomSaveDTO dto) {
        if (!StringUtils.hasText(dto.getCategory())) {
            throw new IllegalArgumentException("品类不能为空");
        }
        if (!StringUtils.hasText(dto.getGenericName())) {
            throw new IllegalArgumentException("统称不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("名称不能为空");
        }
    }

    private boolean isEmptyRow(WarehouseBomImportTemplateRow row) {
        return !StringUtils.hasText(row.getCategory())
                && !StringUtils.hasText(row.getGenericName())
                && !StringUtils.hasText(row.getBrand())
                && !StringUtils.hasText(row.getName())
                && !StringUtils.hasText(row.getRemark());
    }
}
