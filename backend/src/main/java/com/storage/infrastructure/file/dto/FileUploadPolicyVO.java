package com.storage.infrastructure.file.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadPolicyVO {

    private long maxSizeBytes;
}
