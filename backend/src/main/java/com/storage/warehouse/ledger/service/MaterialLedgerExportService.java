package com.storage.warehouse.ledger.service;

import com.storage.warehouse.ledger.entity.MaterialLedger;

import java.io.IOException;
import java.util.List;

public interface MaterialLedgerExportService {
    byte[] export(List<MaterialLedger> records) throws IOException;
}
