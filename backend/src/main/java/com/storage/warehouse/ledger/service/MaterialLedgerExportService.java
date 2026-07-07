package com.storage.warehouse.ledger.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.excel.MaterialLedgerExportRow;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
public interface MaterialLedgerExportService {
    byte[] export(List<MaterialLedger> records) throws IOException;
    byte[] exportTemplate() throws IOException;
}
