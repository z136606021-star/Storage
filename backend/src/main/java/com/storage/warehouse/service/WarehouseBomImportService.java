package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.excel.WarehouseBomExportRow;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
public interface WarehouseBomImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
