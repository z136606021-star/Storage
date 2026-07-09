package com.storage.experience.exception;

public class ExperienceRecordNotFoundException extends RuntimeException {

    public ExperienceRecordNotFoundException(Long id) {
        super("经验记录不存在: " + id);
    }
}
