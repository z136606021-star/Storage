package com.storage.infrastructure.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "storage.file-upload")
public class FileUploadProperties {

    private long maxSizeBytes = 50L * 1024 * 1024;

    private int maxFilesPerRecord = 20;

    private int uploadConcurrency = 3;

    private List<String> allowedContentTypes = new ArrayList<>();
}
