package com.storage.design.exception;

public class DesignGuideNotFoundException extends RuntimeException {

    public DesignGuideNotFoundException(Long id) {
        super("设计指引不存在: " + id);
    }
}
