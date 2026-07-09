package com.storage.infrastructure.file.service;

import com.storage.infrastructure.file.dto.FileUploadVO;
import com.storage.infrastructure.file.dto.FileContentVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadVO upload(MultipartFile file, Long uploaderId);

    String resolveAccessUrl(String objectKey);

    FileContentVO loadImage(String objectKey);

    FileContentVO loadFile(String objectKey);
}
