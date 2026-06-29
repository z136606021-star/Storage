package com.storage.exception;

public class MaterialIoNotFoundException extends RuntimeException {

    public MaterialIoNotFoundException(Long id) {
        super("出入库记录不存在: " + id);
    }
}
