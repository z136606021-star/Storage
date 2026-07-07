package com.storage.warehouse.bom.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.bom.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.bom.excel.WarehouseBomExportRow;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
public interface WarehouseBomImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
