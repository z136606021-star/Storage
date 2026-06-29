package com.storage.service;

import com.storage.entity.SysCustomer;
import com.storage.excel.ExcelCellUtils;
import com.storage.excel.SysCustomerExcelColumn;
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
public class SysCustomerExportService {

    public byte[] export(List<SysCustomer> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<SysCustomer> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("客户");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = SysCustomerExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (SysCustomer record : records) {
                Row row = sheet.createRow(rowIndex);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.INDEX.getIndex(), rowIndex, dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.CUSTOMER_CODE.getIndex(), record.getCustomerCode(), dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.NAME.getIndex(), record.getName(), dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.CONTACT_NAME.getIndex(), record.getContactName(), dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.PHONE.getIndex(), record.getPhone(), dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.EMAIL.getIndex(), record.getEmail(), dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.ADDRESS.getIndex(), record.getAddress(), dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.STATUS.getIndex(), SysCustomerService.formatStatusLabel(record.getStatus()), dataStyle);
                ExcelCellUtils.setCell(row, SysCustomerExcelColumn.REMARK.getIndex(), record.getRemark(), dataStyle);
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
