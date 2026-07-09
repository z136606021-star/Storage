package com.storage.design.service;

import com.storage.common.excel.ExcelExportWriter;
import com.storage.design.entity.DesignProductType;
import com.storage.design.excel.DesignProductTypeExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DesignProductTypeExportServiceImpl implements DesignProductTypeExportService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] export(List<DesignProductType> records) throws IOException {
        List<DesignProductTypeExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (DesignProductType record : records) {
            DesignProductTypeExportRow row = new DesignProductTypeExportRow();
            row.setIndex(rowIndex++);
            row.setTypeCode(record.getTypeCode());
            row.setTypeName(record.getTypeName());
            row.setEnabled(formatEnabled(record.getEnabled()));
            row.setOperatorName(record.getOperatorName());
            row.setOperatedAt(record.getOperatedAt() == null ? "" : FORMATTER.format(record.getOperatedAt()));
            rows.add(row);
        }
        return ExcelExportWriter.writeBytes("产品类型配置", DesignProductTypeExportRow.class, rows);
    }

    private String formatEnabled(Integer enabled) {
        return enabled != null && enabled == 1 ? "启用" : "停用";
    }
}
