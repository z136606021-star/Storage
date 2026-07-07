package com.storage.common.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class ExcelExportWriter {

    private ExcelExportWriter() {
    }

    public static <T> byte[] writeBytes(String sheetName, Class<T> rowClass, List<T> rows) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            EasyExcel.write(out, rowClass)
                    .sheet(sheetName)
                    .registerWriteHandler(ExcelExportStyleHandlers.standardTable())
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .doWrite(rows);
            return out.toByteArray();
        }
    }
}
