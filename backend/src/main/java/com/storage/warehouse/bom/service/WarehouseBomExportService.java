package com.storage.warehouse.bom.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.bom.entity.WarehouseBom;
import com.storage.warehouse.bom.excel.WarehouseBomExportRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public interface WarehouseBomExportService {
    byte[] export(List<WarehouseBom> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
