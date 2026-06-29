package com.storage.service;

import com.storage.dto.SafetyStockRecordVO;
import com.storage.excel.ExcelCellUtils;
import com.storage.excel.SafetyStockExcelColumn;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class SafetyStockExportService {

    public byte[] export(List<SafetyStockRecordVO> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("安全库存");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = SafetyStockExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (SafetyStockRecordVO record : records) {
                Row row = sheet.createRow(rowIndex);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.INDEX.getIndex(), rowIndex, dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.CATEGORY.getIndex(), record.getCategory(), dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.GENERIC_NAME.getIndex(), record.getGenericName(), dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.BRAND.getIndex(), record.getBrand(), dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.NAME.getIndex(), record.getName(), dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.MODEL.getIndex(), record.getModel(), dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.BIN_LOCATION.getIndex(), record.getBinLocation(), dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.STOCK_QUANTITY.getIndex(), record.getStockQuantity(), dataStyle);
                ExcelCellUtils.setCell(row, SafetyStockExcelColumn.SAFETY_QUANTITY.getIndex(), record.getSafetyQuantity(), dataStyle);
                ExcelCellUtils.setCell(
                        row,
                        SafetyStockExcelColumn.WARNING_PERIOD.getIndex(),
                        SafetyStockWarningStatus.formatWarningPeriod(Boolean.TRUE.equals(record.getInWarningPeriod())),
                        dataStyle
                );
                rowIndex++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(width + 512, 256 * 40));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
