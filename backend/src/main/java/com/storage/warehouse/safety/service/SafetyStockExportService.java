package com.storage.warehouse.safety.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.safety.dto.SafetyStockRecordVO;
import com.storage.warehouse.safety.excel.SafetyStockExportRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public interface SafetyStockExportService {
    byte[] export(List<SafetyStockRecordVO> records) throws IOException;
}
