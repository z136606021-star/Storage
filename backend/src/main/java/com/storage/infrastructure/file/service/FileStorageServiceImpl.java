package com.storage.infrastructure.file.service;

import com.storage.infrastructure.file.config.FileUploadProperties;
import com.storage.infrastructure.file.config.MinioProperties;
import com.storage.infrastructure.file.dto.FileUploadVO;
import com.storage.infrastructure.file.entity.SysFile;
import com.storage.common.exception.BusinessException;
import com.storage.infrastructure.file.mapper.SysFileMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final FileUploadProperties fileUploadProperties;
    private final SysFileMapper sysFileMapper;

    @Override
    public FileUploadVO upload(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        validateFile(file);

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
        record.setUploaderId(uploaderId);
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

    @Override
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

    private void validateFile(MultipartFile file) {
        long maxSizeBytes = fileUploadProperties.getMaxSizeBytes();
        if (maxSizeBytes > 0 && file.getSize() > maxSizeBytes) {
            throw new BusinessException("文件大小超过限制");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException("文件类型不正确");
        }

        String normalizedContentType = contentType.trim().toLowerCase(Locale.ROOT);
        boolean allowed = fileUploadProperties.getAllowedContentTypes().stream()
                .filter(StringUtils::hasText)
                .map(type -> type.trim().toLowerCase(Locale.ROOT))
                .anyMatch(type -> type.equals(normalizedContentType));
        if (!allowed) {
            throw new BusinessException("文件类型不支持");
        }
        validateImageSignature(file, normalizedContentType);
    }

    private void validateImageSignature(MultipartFile file, String normalizedContentType) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            boolean valid = switch (normalizedContentType) {
                case "image/jpeg" -> startsWith(header, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                case "image/png" -> startsWith(header, new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                });
                case "image/webp" -> startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                        && header.length >= 12
                        && Arrays.equals(Arrays.copyOfRange(header, 8, 12), "WEBP".getBytes(StandardCharsets.US_ASCII));
                case "image/gif" -> startsWith(header, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                        || startsWith(header, "GIF89a".getBytes(StandardCharsets.US_ASCII));
                default -> false;
            };
            if (!valid) {
                throw new BusinessException("文件内容与类型不匹配");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("文件类型校验失败");
        }
    }

    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
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
