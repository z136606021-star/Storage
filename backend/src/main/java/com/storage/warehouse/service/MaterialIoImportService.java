package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.common.exception.ImportFormatException;
import com.storage.warehouse.dto.MaterialIoSaveDTO;
import com.storage.warehouse.excel.MaterialIoImportTemplateRow;
import com.storage.warehouse.query.MaterialIoQueryBuilder;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.service.MaterialLedgerService;
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
