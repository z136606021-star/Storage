package com.storage.warehouse.bom.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.bom.entity.WarehouseBom;
import com.storage.warehouse.bom.excel.WarehouseBomExcelColumn;
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
public class WarehouseBomExportService {

    public byte[] export(List<WarehouseBom> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<WarehouseBom> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("物料清单");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = WarehouseBomExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (WarehouseBom record : records) {
                Row row = sheet.createRow(rowIndex);
                ExcelCellUtils.setCell(row, WarehouseBomExcelColumn.INDEX.getIndex(), rowIndex, dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBomExcelColumn.CATEGORY.getIndex(), record.getCategory(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBomExcelColumn.GENERIC_NAME.getIndex(), record.getGenericName(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBomExcelColumn.BRAND.getIndex(), record.getBrand(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBomExcelColumn.NAME.getIndex(), record.getName(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBomExcelColumn.REMARK.getIndex(), record.getRemark(), dataStyle);
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
