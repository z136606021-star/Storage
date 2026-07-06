package com.storage.warehouse.ledger.exception;

public class MaterialLedgerNotFoundException extends RuntimeException {

    public MaterialLedgerNotFoundException(Long id) {
        super("物料不存在: " + id);
    }
}
