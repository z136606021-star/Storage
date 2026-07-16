package com.storage.warehouse.service;

import com.storage.warehouse.entity.WarehouseBom;

import java.io.IOException;
import java.util.List;
public interface WarehouseBomExportService {
    byte[] export(List<WarehouseBom> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
