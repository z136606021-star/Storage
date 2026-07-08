package com.storage.warehouse.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.excel.WarehouseBinExportRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public interface WarehouseBinExportService {
    byte[] export(List<WarehouseBin> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
