package com.storage.warehouse.exception;

public class WarehouseBomNotFoundException extends RuntimeException {

    public WarehouseBomNotFoundException(Long id) {
        super("物料清单项不存在: " + id);
    }
}
