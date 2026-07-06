package com.storage.warehouse.bin.service;

import com.storage.warehouse.bin.entity.WarehouseBin;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.bin.excel.WarehouseBinExcelColumn;
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
public class WarehouseBinExportService {

    public byte[] export(List<WarehouseBin> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<WarehouseBin> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bin位");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = WarehouseBinExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (WarehouseBin record : records) {
                Row row = sheet.createRow(rowIndex);
                ExcelCellUtils.setCell(row, WarehouseBinExcelColumn.INDEX.getIndex(), rowIndex, dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBinExcelColumn.BIN_CODE.getIndex(), record.getBinCode(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBinExcelColumn.ROW_NO.getIndex(), record.getRowNo(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBinExcelColumn.COL_NO.getIndex(), record.getColNo(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBinExcelColumn.LEVEL_NO.getIndex(), record.getLevelNo(), dataStyle);
                ExcelCellUtils.setCell(row, WarehouseBinExcelColumn.REMARK.getIndex(), record.getRemark(), dataStyle);
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
