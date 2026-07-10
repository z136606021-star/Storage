package com.storage.infrastructure.file.service;

import com.storage.infrastructure.file.dto.FileContentVO;
import com.storage.infrastructure.file.dto.FileUploadPolicyVO;
import com.storage.infrastructure.file.dto.FileUploadVO;
import com.storage.infrastructure.file.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadVO upload(MultipartFile file, Long uploaderId);

    FileUploadPolicyVO uploadPolicy();

    String resolveAccessUrl(String objectKey);

    FileContentVO loadImage(String objectKey);

    FileContentVO loadFile(String objectKey);

    SysFile requireFileRecord(String objectKey);

    void assertImageFile(String objectKey);

    void assertAllowedFile(String objectKey);
}
