package com.storage.warehouse.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.MaterialIoRecordVO;
import com.storage.warehouse.excel.MaterialIoExportRow;
import com.storage.warehouse.excel.MaterialIoImportTemplateRow;
import com.storage.warehouse.query.MaterialIoQueryBuilder;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
public interface MaterialIoExportService {
    byte[] export(List<MaterialIoRecordVO> records) throws IOException;
    byte[] exportTemplate() throws IOException;
    byte[] exportImportTemplate() throws IOException;
}
