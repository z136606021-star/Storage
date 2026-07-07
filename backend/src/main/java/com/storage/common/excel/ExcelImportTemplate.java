package com.storage.common.excel;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.exception.ImportFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ExcelImportTemplate {

    private ExcelImportTemplate() {
    }

    public static ImportResultVO importRows(MultipartFile file, RowEmptyChecker emptyChecker, RowImporter importer)
            throws IOException {
        ImportResultVO result = new ImportResultVO();
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();

        forEachDataRow(file, emptyChecker, (excelRow, row) -> {
            try {
                importer.importRow(excelRow, row);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception ex) {
                result.setFailCount(result.getFailCount() + 1);
                errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));
            }
        });

        result.setErrors(errors);
        return result;
    }

    public static <T> ParsedRows<T> parseRows(MultipartFile file, RowEmptyChecker emptyChecker, RowParser<T> parser)
            throws IOException {
        List<ParsedRow<T>> rows = new ArrayList<>();
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();

        forEachDataRow(file, emptyChecker, (excelRow, row) -> {
            try {
                rows.add(new ParsedRow<>(excelRow, parser.parse(excelRow, row)));
            } catch (Exception ex) {
                errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));
            }
        });

        return new ParsedRows<>(rows, errors);
    }

    private static void forEachDataRow(MultipartFile file, RowEmptyChecker emptyChecker, RowVisitor visitor)
            throws IOException {
        validateFile(file);

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ImportFormatException("Excel 文件中没有工作表");
            }

            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || emptyChecker.isEmpty(row)) {
                    continue;
                }
                visitor.visit(i + 1, row);
            }
        }
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
    public interface RowEmptyChecker {
        boolean isEmpty(Row row);
    }

    @FunctionalInterface
    public interface RowImporter {
        void importRow(int excelRow, Row row) throws Exception;
    }

    @FunctionalInterface
    public interface RowParser<T> {
        T parse(int excelRow, Row row) throws Exception;
    }

    @FunctionalInterface
    private interface RowVisitor {
        void visit(int excelRow, Row row) throws IOException;
    }

    public record ParsedRow<T>(int excelRow, T value) {
    }

    public record ParsedRows<T>(List<ParsedRow<T>> rows, List<ImportResultVO.ImportErrorVO> errors) {
    }
}
