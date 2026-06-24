package com.storage.service;

import com.storage.dto.SysUserVO;
import com.storage.excel.ExcelCellUtils;
import com.storage.excel.SysUserExcelColumn;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysUserExportService {

    public byte[] export(List<SysUserVO> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = SysUserExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (SysUserVO record : records) {
                Row row = sheet.createRow(rowIndex++);
                ExcelCellUtils.setCell(row, SysUserExcelColumn.NTID.getIndex(), record.getUsername(), dataStyle);
                ExcelCellUtils.setCell(row, SysUserExcelColumn.DISPLAY_NAME.getIndex(), record.getDisplayName(), dataStyle);
                ExcelCellUtils.setCell(row, SysUserExcelColumn.EMAIL.getIndex(), record.getEmail(), dataStyle);
                ExcelCellUtils.setCell(row, SysUserExcelColumn.PHONE.getIndex(), record.getPhone(), dataStyle);
                String roleCodes = record.getRoleCodes() == null ? "" : String.join(",", record.getRoleCodes());
                ExcelCellUtils.setCell(row, SysUserExcelColumn.ROLE_CODES.getIndex(), roleCodes, dataStyle);
                ExcelCellUtils.setCell(row, SysUserExcelColumn.STATUS.getIndex(), formatStatus(record.getStatus()), dataStyle);
            }

            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTemplate() throws IOException {
        return export(List.of());
    }

    private String formatStatus(Integer status) {
        return status != null && status == 1 ? "启用" : "停用";
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 512, 256 * 40));
        }
    }
}
