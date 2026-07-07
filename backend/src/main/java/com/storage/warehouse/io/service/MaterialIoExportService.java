package com.storage.warehouse.io.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.io.dto.MaterialIoRecordVO;
import com.storage.warehouse.io.excel.MaterialIoExportRow;
import com.storage.warehouse.io.excel.MaterialIoImportTemplateRow;
import com.storage.warehouse.io.query.MaterialIoQueryBuilder;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
public interface MaterialIoExportService {
    byte[] export(List<MaterialIoRecordVO> records) throws IOException;
    byte[] exportTemplate() throws IOException;
    byte[] exportImportTemplate() throws IOException;
}
