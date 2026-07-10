package com.storage.infrastructure.file.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FileUploadPolicyVO {

    private long maxSizeBytes;

    private int maxFilesPerRecord;

    private int uploadConcurrency;

    private List<String> allowedContentTypes;

    private List<String> imageContentTypes;
}
