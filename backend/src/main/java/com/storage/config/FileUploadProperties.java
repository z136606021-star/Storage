package com.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "storage.file-upload")
public class FileUploadProperties {

    private long maxSizeBytes = 5 * 1024 * 1024;

    private List<String> allowedContentTypes = new ArrayList<>();
}
