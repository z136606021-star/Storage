package com.storage.infrastructure.file.service;

import com.storage.infrastructure.file.dto.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadVO upload(MultipartFile file, Long uploaderId);

    String resolvePresignedUrl(String objectKey);
}
