package com.storage.warehouse.bin.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.bin.entity.WarehouseBin;
import com.storage.warehouse.bin.excel.WarehouseBinExportRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public interface WarehouseBinExportService {
    byte[] export(List<WarehouseBin> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
