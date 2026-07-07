package com.storage.system.customer.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.system.customer.dto.SysCustomerSaveDTO;
import com.storage.system.customer.excel.SysCustomerExportRow;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
public interface SysCustomerImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
