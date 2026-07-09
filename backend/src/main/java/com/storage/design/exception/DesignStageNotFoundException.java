package com.storage.design.exception;

public class DesignStageNotFoundException extends RuntimeException {

    public DesignStageNotFoundException(Long id) {
        super("项目阶段不存在: " + id);
    }
}
