package com.storage.service;

import com.storage.dto.ImportResultVO;
import com.storage.dto.WarehouseBinSaveDTO;
import com.storage.excel.ExcelCellUtils;
import com.storage.excel.WarehouseBinExcelColumn;
import com.storage.exception.ImportFormatException;
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
public class WarehouseBinImportService {

    private final WarehouseBinService warehouseBinService;

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
                    WarehouseBinSaveDTO dto = parseRow(row);
                    warehouseBinService.create(dto);
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

    private WarehouseBinSaveDTO parseRow(Row row) {
        WarehouseBinSaveDTO dto = new WarehouseBinSaveDTO();
        dto.setRowNo(parsePositiveInt(ExcelCellUtils.getCellString(row, WarehouseBinExcelColumn.ROW_NO.getIndex()), "排"));
        dto.setColNo(parsePositiveInt(ExcelCellUtils.getCellString(row, WarehouseBinExcelColumn.COL_NO.getIndex()), "列"));
        dto.setLevelNo(parsePositiveInt(ExcelCellUtils.getCellString(row, WarehouseBinExcelColumn.LEVEL_NO.getIndex()), "层"));
        dto.setRemark(ExcelCellUtils.getCellString(row, WarehouseBinExcelColumn.REMARK.getIndex()));
        return dto;
    }

    private Integer parsePositiveInt(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1) {
                throw new IllegalArgumentException(label + "必须为正整数");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + "格式不正确");
        }
    }

    private boolean isEmptyRow(Row row) {
        for (WarehouseBinExcelColumn column : WarehouseBinExcelColumn.values()) {
            if (column == WarehouseBinExcelColumn.INDEX || column == WarehouseBinExcelColumn.BIN_CODE) {
                continue;
            }
            if (StringUtils.hasText(ExcelCellUtils.getCellString(row, column.getIndex()))) {
                return false;
            }
        }
        return true;
    }
}
