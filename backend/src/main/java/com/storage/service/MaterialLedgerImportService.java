package com.storage.service;

import com.storage.dto.ImportResultVO;
import com.storage.dto.MaterialSaveDTO;
import com.storage.entity.MaterialLedger;
import com.storage.exception.ImportFormatException;
import com.storage.mapper.MaterialLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialLedgerImportService {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private final MaterialLedgerMapper materialLedgerMapper;

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
                    MaterialSaveDTO dto = parseRow(row);
                    validateDto(dto);
                    materialLedgerMapper.insert(toEntity(dto));
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

    private MaterialSaveDTO parseRow(Row row) {
        MaterialSaveDTO dto = new MaterialSaveDTO();
        dto.setCategory(getCellString(row, 1));
        dto.setGenericName(getCellString(row, 2));
        dto.setBrand(getCellString(row, 3));
        dto.setName(getCellString(row, 4));
        dto.setModel(getCellString(row, 5));
        dto.setBinLocation(getCellString(row, 6));
        dto.setUnitPrice(parseUnitPrice(getCellString(row, 8)));
        dto.setRemark(getCellString(row, 9));
        return dto;
    }

    private void validateDto(MaterialSaveDTO dto) {
        if (!StringUtils.hasText(dto.getCategory())) {
            throw new IllegalArgumentException("品类不能为空");
        }
        if (!StringUtils.hasText(dto.getGenericName())) {
            throw new IllegalArgumentException("统称不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (!StringUtils.hasText(dto.getModel())) {
            throw new IllegalArgumentException("型号不能为空");
        }
        if (!StringUtils.hasText(dto.getBinLocation())) {
            throw new IllegalArgumentException("Bin位不能为空");
        }
    }

    private MaterialLedger toEntity(MaterialSaveDTO dto) {
        MaterialLedger entity = new MaterialLedger();
        entity.setCategory(dto.getCategory().trim());
        entity.setGenericName(dto.getGenericName().trim());
        entity.setBrand(trimToNull(dto.getBrand()));
        entity.setName(dto.getName().trim());
        entity.setModel(dto.getModel().trim());
        entity.setBinLocation(dto.getBinLocation().trim());
        entity.setStockQuantity(0);
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setRemark(trimToNull(dto.getRemark()));
        return entity;
    }

    private String getCellString(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            return null;
        }
        String value = FORMATTER.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    private BigDecimal parseUnitPrice(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("单价格式不正确");
        }
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 1; i <= 9; i++) {
            if (StringUtils.hasText(getCellString(row, i))) {
                return false;
            }
        }
        return true;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
