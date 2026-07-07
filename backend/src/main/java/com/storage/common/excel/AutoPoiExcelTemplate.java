package com.storage.common.excel;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.exception.ImportFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AutoPoiExcelTemplate {

    private AutoPoiExcelTemplate() {
    }

    public static <T> byte[] exportBytes(String sheetName, Class<T> rowClass, List<T> rows) throws IOException {
        ExportParams params = new ExportParams(null, sheetName, ExcelType.XSSF);
        params.setCreateHeadRows(true);
        try (Workbook workbook = ExcelExportUtil.exportExcel(params, rowClass, rows);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public static <T> ImportResultVO importRows(
            MultipartFile file,
            Class<T> rowClass,
            Predicate<T> emptyChecker,
            RowImporter<T> importer
    ) throws IOException {
        ParsedRows<T> parsedRows = parseRows(file, rowClass, emptyChecker, (excelRow, row) -> row);

        ImportResultVO result = new ImportResultVO();
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>(parsedRows.errors());
        for (ParsedRow<T> row : parsedRows.rows()) {
            int excelRow = row.excelRow();
            try {
                importer.importRow(excelRow, row.value());
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception ex) {
                result.setFailCount(result.getFailCount() + 1);
                errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));
            }
        }
        result.setErrors(errors);
        return result;
    }

    public static <T, R> ParsedRows<R> parseRows(
            MultipartFile file,
            Class<T> rowClass,
            Predicate<T> emptyChecker,
            RowParser<T, R> parser
    ) throws IOException {
        List<ExcelRow<T>> rows = readRowsWithNumbers(file, rowClass).stream()
                .filter(row -> !emptyChecker.test(row.value()))
                .toList();

        List<ParsedRow<R>> parsedRows = new ArrayList<>();
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
        for (ExcelRow<T> row : rows) {
            int excelRow = row.excelRow();
            try {
                parsedRows.add(new ParsedRow<>(excelRow, parser.parse(excelRow, row.value())));
            } catch (Exception ex) {
                errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));
            }
        }
        return new ParsedRows<>(parsedRows, errors);
    }

    private static <T> List<ExcelRow<T>> readRowsWithNumbers(MultipartFile file, Class<T> rowClass) throws IOException {
        List<T> rows = readRows(file, rowClass);
        List<Integer> rowNumbers = readDataRowNumbers(file);
        List<ExcelRow<T>> numberedRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int excelRow = i < rowNumbers.size() ? rowNumbers.get(i) : i + 2;
            numberedRows.add(new ExcelRow<>(excelRow, rows.get(i)));
        }
        return numberedRows;
    }

    private static <T> List<T> readRows(MultipartFile file, Class<T> rowClass) throws IOException {
        validateFile(file);
        ImportParams params = new ImportParams();
        params.setTitleRows(0);
        params.setHeadRows(1);
        try {
            return ExcelImportUtil.importExcel(file.getInputStream(), rowClass, params);
        } catch (Exception ex) {
            throw new ImportFormatException("Excel 解析失败: " + ex.getMessage());
        }
    }

    private static List<Integer> readDataRowNumbers(MultipartFile file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<Integer> rowNumbers = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ImportFormatException("Excel 文件中没有工作表");
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && hasCellValue(row, formatter)) {
                    rowNumbers.add(i + 1);
                }
            }
        } catch (ImportFormatException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ImportFormatException("Excel 解析失败: " + ex.getMessage());
        }
        return rowNumbers;
    }

    private static boolean hasCellValue(Row row, DataFormatter formatter) {
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            return false;
        }
        for (int i = 0; i < lastCellNum; i++) {
            if (!formatter.formatCellValue(row.getCell(i)).trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImportFormatException("请上传 Excel 文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new ImportFormatException("仅支持 .xlsx 或 .xls 格式");
        }
    }

    @FunctionalInterface
    public interface RowImporter<T> {
        void importRow(int excelRow, T row) throws Exception;
    }

    @FunctionalInterface
    public interface RowParser<T, R> {
        R parse(int excelRow, T row) throws Exception;
    }

    public record ParsedRow<T>(int excelRow, T value) {
    }

    public record ParsedRows<T>(List<ParsedRow<T>> rows, List<ImportResultVO.ImportErrorVO> errors) {
    }

    private record ExcelRow<T>(int excelRow, T value) {
    }
}
