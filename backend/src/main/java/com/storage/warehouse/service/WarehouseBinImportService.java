package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.excel.WarehouseBinExportRow;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
public interface WarehouseBinImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
