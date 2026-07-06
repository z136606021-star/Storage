package com.storage.warehouse.ledger.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.common.exception.ImportFormatException;
import com.storage.warehouse.bom.service.WarehouseBomService;
import com.storage.warehouse.bin.service.WarehouseBinService;
import com.storage.warehouse.ledger.converter.MaterialLedgerConverter;
import com.storage.warehouse.ledger.dto.MaterialSaveDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.excel.MaterialLedgerExcelColumn;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import lombok.RequiredArgsConstructor;
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

    private final MaterialLedgerMapper materialLedgerMapper;
    private final MaterialLedgerConverter materialLedgerConverter;
    private final WarehouseBinService warehouseBinService;
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
                    MaterialSaveDTO dto = parseRow(row);
                    validateDto(dto);
                    warehouseBinService.assertBinExists(dto.getBinLocation());
                    warehouseBomService.assertCatalogExists(
                            dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName());
                    MaterialLedger entity = materialLedgerConverter.toNewEntity(dto);
                    materialLedgerMapper.insert(entity);
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
        dto.setCategory(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.CATEGORY.getIndex()));
        dto.setGenericName(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.GENERIC_NAME.getIndex()));
        dto.setBrand(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.BRAND.getIndex()));
        dto.setName(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.NAME.getIndex()));
        dto.setModel(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.MODEL.getIndex()));
        dto.setBinLocation(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.BIN_LOCATION.getIndex()));
        dto.setUnitPrice(parseUnitPrice(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.UNIT_PRICE.getIndex())));
        dto.setRemark(ExcelCellUtils.getCellString(row, MaterialLedgerExcelColumn.REMARK.getIndex()));
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
        for (MaterialLedgerExcelColumn column : MaterialLedgerExcelColumn.values()) {
            if (column == MaterialLedgerExcelColumn.INDEX) {
                continue;
            }
            if (StringUtils.hasText(ExcelCellUtils.getCellString(row, column.getIndex()))) {
                return false;
            }
        }
        return true;
    }
}
