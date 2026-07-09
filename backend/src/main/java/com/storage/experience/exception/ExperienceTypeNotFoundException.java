package com.storage.experience.exception;

public class ExperienceTypeNotFoundException extends RuntimeException {

    public ExperienceTypeNotFoundException(Long id) {
        super("经验类型不存在: " + id);
    }
}
