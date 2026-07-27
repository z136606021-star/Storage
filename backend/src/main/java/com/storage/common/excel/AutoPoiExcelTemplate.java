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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public static <T> ImportResultVO importRows(
            MultipartFile file,
            Class<T> rowClass,
            Predicate<T> emptyChecker,
            RowImporter<T> importer,
            Map<List<String>, List<String>> headerMappings
    ) throws IOException {
        MultipartFile normalized = normalizeHeaders(file, headerMappings);
        return importRows(normalized, rowClass, emptyChecker, importer);
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

    public static <T, R> ParsedRows<R> parseRows(
            MultipartFile file,
            Class<T> rowClass,
            Predicate<T> emptyChecker,
            RowParser<T, R> parser,
            Map<List<String>, List<String>> headerMappings
    ) throws IOException {
        MultipartFile normalized = normalizeHeaders(file, headerMappings);
        return parseRows(normalized, rowClass, emptyChecker, parser);
    }

    private static MultipartFile normalizeHeaders(
            MultipartFile file,
            Map<List<String>, List<String>> headerMappings
    ) throws IOException {
        validateFile(file);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null || sheet.getRow(0) == null) {
                throw new ImportFormatException("Excel 文件中没有表头");
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            List<String> actual = new ArrayList<>();
            for (int i = 0; i < header.getLastCellNum(); i++) {
                actual.add(formatter.formatCellValue(header.getCell(i)).trim());
            }
            List<String> normalizedHeaders = headerMappings.get(actual);
            if (normalizedHeaders != null) {
                while (actual.size() > normalizedHeaders.size()) {
                    int removeIndex = actual.size() - 1;
                    if (actual.contains("统称")) {
                        removeIndex = 5;
                    }
                    for (int candidate = actual.contains("统称") ? actual.size() : 0; candidate < actual.size(); candidate++) {
                        if (candidate < normalizedHeaders.size()
                                && !actual.get(candidate).equals(normalizedHeaders.get(candidate))
                                && candidate + 1 < actual.size()
                                && actual.get(candidate + 1).equals(normalizedHeaders.get(candidate))) {
                            removeIndex = candidate;
                            break;
                        }
                    }
                    for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row == null) {
                            continue;
                        }
                        for (int column = removeIndex; column < row.getLastCellNum() - 1; column++) {
                            row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                                    .setCellValue(formatter.formatCellValue(row.getCell(column + 1)));
                        }
                        row.removeCell(row.getCell(row.getLastCellNum() - 1));
                    }
                    actual.remove(removeIndex);
                }
                for (int i = 0; i < normalizedHeaders.size(); i++) {
                    header.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(normalizedHeaders.get(i));
                }
            }
            workbook.write(out);
            byte[] bytes = out.toByteArray();
            return new InMemoryMultipartFile(file, bytes);
        } catch (ImportFormatException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ImportFormatException("Excel 解析失败: " + ex.getMessage());
        }
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

    private record InMemoryMultipartFile(MultipartFile source, byte[] content) implements MultipartFile {
        @Override public String getName() { return source.getName(); }
        @Override public String getOriginalFilename() { return source.getOriginalFilename(); }
        @Override public String getContentType() { return source.getContentType(); }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public java.io.InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException { java.nio.file.Files.write(dest.toPath(), content); }
    }
    private record ExcelRow<T>(int excelRow, T value) {
    }
}
