package com.storage.warehouse.service;

import com.storage.warehouse.dto.SafetyStockRecordVO;

import java.io.IOException;
import java.util.List;

public interface SafetyStockExportService {
    byte[] export(List<SafetyStockRecordVO> records) throws IOException;

    byte[] exportPurchaseList(List<SafetyStockRecordVO> records) throws IOException;
}
