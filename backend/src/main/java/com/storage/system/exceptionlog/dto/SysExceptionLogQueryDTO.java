package com.storage.system.exceptionlog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysExceptionLogQueryDTO {

    private String source;

    private Integer httpStatus;

    private String exceptionClass;

    private String requestId;

    private String requestPath;

    private String keyword;

    private LocalDateTime occurredAtStart;

    private LocalDateTime occurredAtEnd;

    private Integer page = 1;

    private Integer pageSize = 10;
}
