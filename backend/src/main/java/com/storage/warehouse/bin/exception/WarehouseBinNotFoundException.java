package com.storage.warehouse.bin.exception;

public class WarehouseBinNotFoundException extends RuntimeException {

    public WarehouseBinNotFoundException(Long id) {
        super("Bin位不存在: " + id);
    }
}
