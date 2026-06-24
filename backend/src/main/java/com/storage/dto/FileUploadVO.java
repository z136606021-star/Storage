package com.storage.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadVO {

    private Long id;

    private String objectKey;

    private String originalName;

    private String contentType;

    private Long sizeBytes;

    private String url;
}
