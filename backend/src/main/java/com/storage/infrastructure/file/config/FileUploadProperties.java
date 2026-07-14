package com.storage.infrastructure.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.file-upload")
public class FileUploadProperties {

    private long maxSizeBytes = 5_505_025_024L;
}
