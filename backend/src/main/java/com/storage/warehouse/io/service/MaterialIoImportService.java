package com.storage.warehouse.io.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.common.exception.ImportFormatException;
import com.storage.warehouse.io.dto.MaterialIoSaveDTO;
import com.storage.warehouse.io.excel.MaterialIoImportTemplateRow;
import com.storage.warehouse.io.query.MaterialIoQueryBuilder;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.service.MaterialLedgerService;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
public interface MaterialIoImportService {
    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
