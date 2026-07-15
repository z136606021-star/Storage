package com.storage.system.exceptionlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_exception_log")
public class SysExceptionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String source;

    private String level;

    private LocalDateTime occurredAt;

    private String errorCode;

    private String requestId;

    private Integer httpStatus;

    private String httpMethod;

    private String requestPath;

    private String exceptionClass;

    private String summary;

    private String stackTrace;

    private String frontendRoute;

    private String browserInfo;

    private Long operatorId;

    private String operatorUsername;

    private LocalDateTime createdAt;
}
