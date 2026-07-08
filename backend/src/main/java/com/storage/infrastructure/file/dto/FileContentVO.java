package com.storage.infrastructure.file.dto;

import java.io.InputStream;

public record FileContentVO(
        InputStream inputStream,
        String originalName,
        String contentType,
        Long sizeBytes
) {
}
