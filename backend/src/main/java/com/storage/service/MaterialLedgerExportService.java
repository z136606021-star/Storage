package com.storage.service;

import com.storage.entity.MaterialLedger;
import com.storage.excel.ExcelCellUtils;
import com.storage.excel.MaterialLedgerExcelColumn;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class MaterialLedgerExportService {

    public byte[] export(List<MaterialLedger> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<MaterialLedger> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("物料台账");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = MaterialLedgerExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (MaterialLedger record : records) {
                Row row = sheet.createRow(rowIndex);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.INDEX.getIndex(), rowIndex, dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.CATEGORY.getIndex(), record.getCategory(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.GENERIC_NAME.getIndex(), record.getGenericName(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.BRAND.getIndex(), record.getBrand(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.NAME.getIndex(), record.getName(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.MODEL.getIndex(), record.getModel(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.BIN_LOCATION.getIndex(), record.getBinLocation(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.STOCK_QUANTITY.getIndex(), record.getStockQuantity(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.UNIT_PRICE.getIndex(), formatUnitPrice(record.getUnitPrice()), dataStyle);
                ExcelCellUtils.setCell(row, MaterialLedgerExcelColumn.REMARK.getIndex(), record.getRemark(), dataStyle);
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

    private String formatUnitPrice(BigDecimal unitPrice) {
        return unitPrice == null ? "" : unitPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
