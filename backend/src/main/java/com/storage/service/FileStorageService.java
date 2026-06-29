package com.storage.service;

import com.storage.config.MinioProperties;
import com.storage.dto.FileUploadVO;
import com.storage.entity.SysFile;
import com.storage.entity.SysUser;
import com.storage.exception.BusinessException;
import com.storage.mapper.SysFileMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final SysFileMapper sysFileMapper;

    public FileUploadVO upload(MultipartFile file, SysUser uploader) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String objectKey = buildObjectKey(originalName);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception ex) {
            throw new BusinessException("文件上传失败，请确认 MinIO 服务已启动");
        }

        SysFile record = new SysFile();
        record.setObjectKey(objectKey);
        record.setOriginalName(originalName);
        record.setContentType(file.getContentType());
        record.setSizeBytes(file.getSize());
        record.setUploaderId(uploader == null ? null : uploader.getId());
        sysFileMapper.insert(record);

        return FileUploadVO.builder()
                .id(record.getId())
                .objectKey(objectKey)
                .originalName(originalName)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .url(resolvePresignedUrl(objectKey))
                .build();
    }

    public String resolvePresignedUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return buildPresignedUrl(objectKey.trim());
    }

    private String buildObjectKey(String originalName) {
        String safeName = originalName.replaceAll("[\\\\/]+", "_");
        return LocalDate.now() + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private String buildPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception ex) {
            return null;
        }
    }
}
