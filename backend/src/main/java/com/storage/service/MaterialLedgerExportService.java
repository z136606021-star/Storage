package com.storage.service;

import com.storage.entity.MaterialLedger;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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

    private static final String[] HEADERS = {
            "序号", "品类", "统称", "品牌", "名称", "型号", "Bin位", "库存数量", "单价", "备注"
    };

    public byte[] export(List<MaterialLedger> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<MaterialLedger> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("物料台账");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (MaterialLedger record : records) {
                Row row = sheet.createRow(rowIndex);
                setCell(row, 0, rowIndex, dataStyle);
                setCell(row, 1, record.getCategory(), dataStyle);
                setCell(row, 2, record.getGenericName(), dataStyle);
                setCell(row, 3, record.getBrand(), dataStyle);
                setCell(row, 4, record.getName(), dataStyle);
                setCell(row, 5, record.getModel(), dataStyle);
                setCell(row, 6, record.getBinLocation(), dataStyle);
                setCell(row, 7, record.getStockQuantity(), dataStyle);
                setCell(row, 8, formatUnitPrice(record.getUnitPrice()), dataStyle);
                setCell(row, 9, record.getRemark(), dataStyle);
                rowIndex++;
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(width + 512, 256 * 40));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void setCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private String formatUnitPrice(BigDecimal unitPrice) {
        return unitPrice == null ? "" : unitPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
