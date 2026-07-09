package com.storage.design.exception;

public class DesignProductTypeNotFoundException extends RuntimeException {

    public DesignProductTypeNotFoundException(Long id) {
        super("产品类型不存在: " + id);
    }
}
