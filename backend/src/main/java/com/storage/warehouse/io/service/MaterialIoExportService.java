package com.storage.warehouse.io.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.warehouse.io.dto.MaterialIoRecordVO;
import com.storage.warehouse.io.excel.MaterialIoExcelColumn;
import com.storage.warehouse.io.query.MaterialIoQueryBuilder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MaterialIoExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] export(List<MaterialIoRecordVO> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportImportTemplate();
    }

    public byte[] exportImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("物料出入库导入");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            String[] headers = MaterialIoExcelColumn.importTemplateHeaders();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] exportWorkbook(List<MaterialIoRecordVO> records) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("物料出入库");
            CellStyle headerStyle = ExcelCellUtils.createHeaderStyle(workbook);
            CellStyle dataStyle = ExcelCellUtils.createDataStyle(workbook);

            String[] headers = MaterialIoExcelColumn.headers();
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                ExcelCellUtils.setCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 1;
            for (MaterialIoRecordVO record : records) {
                Row row = sheet.createRow(rowIndex);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.INDEX.getIndex(), rowIndex, dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.CATEGORY.getIndex(), record.getCategory(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.GENERIC_NAME.getIndex(), record.getGenericName(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.BRAND.getIndex(), record.getBrand(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.NAME.getIndex(), record.getName(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.MODEL.getIndex(), record.getModel(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.BIN_LOCATION.getIndex(), record.getBinLocation(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.QUANTITY.getIndex(), record.getQuantity(), dataStyle);
                ExcelCellUtils.setCell(
                        row,
                        MaterialIoExcelColumn.PURPOSE.getIndex(),
                        MaterialIoPurpose.purposeLabel(record.getPurpose()),
                        dataStyle
                );
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.REMARK.getIndex(), record.getRemark(), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.IO_TYPE.getIndex(), MaterialIoQueryBuilder.ioTypeLabel(record.getIoType()), dataStyle);
                ExcelCellUtils.setCell(row, MaterialIoExcelColumn.OPERATOR.getIndex(), formatOperator(record), dataStyle);
                ExcelCellUtils.setCell(
                        row,
                        MaterialIoExcelColumn.OPERATED_AT.getIndex(),
                        record.getOperatedAt() == null ? "" : DATE_TIME_FORMATTER.format(record.getOperatedAt()),
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

    private String formatOperator(MaterialIoRecordVO record) {
        if (record.getOperatorDisplayName() != null && !record.getOperatorDisplayName().isBlank()) {
            return record.getOperatorDisplayName();
        }
        return record.getOperatorUsername() == null ? "" : record.getOperatorUsername();
    }
}
