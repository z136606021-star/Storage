package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.excel.WarehouseBinExportRow;
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
        return AutoPoiExcelTemplate.importRows(file, WarehouseBinExportRow.class, this::isEmptyRow, (excelRow, row) ->
                warehouseBinService.create(parseRow(row)));
    }

    private WarehouseBinSaveDTO parseRow(WarehouseBinExportRow row) {
        WarehouseBinSaveDTO dto = new WarehouseBinSaveDTO();
        dto.setRowNo(parsePositiveInt(row.getRowNo(), "排"));
        dto.setColNo(parsePositiveInt(row.getColNo(), "列"));
        dto.setLevelNo(parsePositiveInt(row.getLevelNo(), "层"));
        dto.setRemark(row.getRemark());
        return dto;
    }

    private Integer parsePositiveInt(Integer value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        if (value < 1) {
            throw new IllegalArgumentException(label + "必须为正整数");
        }
        return value;
    }

    private boolean isEmptyRow(WarehouseBinExportRow row) {
        return row.getRowNo() == null
                && row.getColNo() == null
                && row.getLevelNo() == null
                && !StringUtils.hasText(row.getRemark());
    }
}
