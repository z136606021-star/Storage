package com.storage.exception;

public class SysCustomerNotFoundException extends RuntimeException {

    public SysCustomerNotFoundException(Long id) {
        super("客户不存在: " + id);
    }
}
