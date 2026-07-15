package com.storage.system.exceptionlog.exception;

public class SysExceptionLogNotFoundException extends RuntimeException {

    public SysExceptionLogNotFoundException(Long id) {
        super("异常日志不存在: " + id);
    }
}
