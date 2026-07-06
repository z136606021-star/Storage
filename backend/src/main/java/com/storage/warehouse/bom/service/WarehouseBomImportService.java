package com.storage.warehouse.bom.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.common.exception.ImportFormatException;
import com.storage.warehouse.bom.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.bom.excel.WarehouseBomExcelColumn;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseBomImportService {

    private final WarehouseBomService warehouseBomService;

    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ImportFormatException("请上传 Excel 文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new ImportFormatException("仅支持 .xlsx 或 .xls 格式");
        }

        ImportResultVO result = new ImportResultVO();
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ImportFormatException("Excel 文件中没有工作表");
            }

            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                int excelRow = i + 1;
                try {
                    WarehouseBomSaveDTO dto = parseRow(row);
                    validateDto(dto);
                    warehouseBomService.create(dto);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));
                }
            }
        }

        result.setErrors(errors);
        return result;
    }

    private WarehouseBomSaveDTO parseRow(Row row) {
        WarehouseBomSaveDTO dto = new WarehouseBomSaveDTO();
        dto.setCategory(ExcelCellUtils.getCellString(row, WarehouseBomExcelColumn.CATEGORY.getIndex()));
        dto.setGenericName(ExcelCellUtils.getCellString(row, WarehouseBomExcelColumn.GENERIC_NAME.getIndex()));
        dto.setBrand(ExcelCellUtils.getCellString(row, WarehouseBomExcelColumn.BRAND.getIndex()));
        dto.setName(ExcelCellUtils.getCellString(row, WarehouseBomExcelColumn.NAME.getIndex()));
        dto.setRemark(ExcelCellUtils.getCellString(row, WarehouseBomExcelColumn.REMARK.getIndex()));
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

    private boolean isEmptyRow(Row row) {
        for (WarehouseBomExcelColumn column : WarehouseBomExcelColumn.values()) {
            if (column == WarehouseBomExcelColumn.INDEX) {
                continue;
            }
            if (StringUtils.hasText(ExcelCellUtils.getCellString(row, column.getIndex()))) {
                return false;
            }
        }
        return true;
    }
}
