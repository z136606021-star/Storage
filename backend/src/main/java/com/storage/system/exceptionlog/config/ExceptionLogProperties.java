package com.storage.system.exceptionlog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.exception-log")
public class ExceptionLogProperties {

    private int retentionDays = 30;

    private int maxStackTraceLength = 8000;

    private int maxSummaryLength = 500;
}
