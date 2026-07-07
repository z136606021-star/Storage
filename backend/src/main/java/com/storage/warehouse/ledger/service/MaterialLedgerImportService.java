package com.storage.warehouse.ledger.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.bom.service.WarehouseBomService;
import com.storage.warehouse.bin.service.WarehouseBinService;
import com.storage.warehouse.ledger.converter.MaterialLedgerConverter;
import com.storage.warehouse.ledger.dto.MaterialSaveDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.excel.MaterialLedgerExportRow;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
public interface MaterialLedgerImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
