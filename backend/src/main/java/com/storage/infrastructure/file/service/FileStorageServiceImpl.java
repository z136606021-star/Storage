package com.storage.infrastructure.file.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.exception.BusinessException;
import com.storage.infrastructure.file.config.FileUploadProperties;
import com.storage.infrastructure.file.config.MinioProperties;
import com.storage.infrastructure.file.dto.FileContentVO;
import com.storage.infrastructure.file.dto.FileUploadPolicyVO;
import com.storage.infrastructure.file.dto.FileUploadVO;
import com.storage.infrastructure.file.entity.SysFile;
import com.storage.infrastructure.file.mapper.SysFileMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final int IMAGE_SIGNATURE_LENGTH = 12;

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
        String contentType = normalizeContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            throw new BusinessException("文件上传失败：" + detail);
        }

        SysFile record = new SysFile();
        record.setObjectKey(objectKey);
        record.setOriginalName(originalName);
        record.setContentType(contentType);
        record.setSizeBytes(file.getSize());
        record.setUploaderId(uploaderId);
        sysFileMapper.insert(record);

        return FileUploadVO.builder()
                .id(record.getId())
                .objectKey(objectKey)
                .originalName(originalName)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .url(resolveAccessUrl(objectKey))
                .build();
    }

    @Override
    public FileUploadPolicyVO uploadPolicy() {
        return FileUploadPolicyVO.builder()
                .maxSizeBytes(fileUploadProperties.getMaxSizeBytes())
                .build();
    }

    @Override
    public String resolveAccessUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        String encoded = URLEncoder.encode(objectKey.trim(), StandardCharsets.UTF_8);
        return "/api/files/preview?objectKey=" + encoded;
    }

    @Override
    public FileContentVO loadImage(String objectKey) {
        SysFile record = findFileRecord(objectKey);
        if (!isImageContentType(record.getContentType())) {
            throw new BusinessException("文件类型不支持预览");
        }
        validateImageSignature(record, "文件类型不支持预览");
        return loadObject(record);
    }

    @Override
    public FileContentVO loadFile(String objectKey) {
        return loadObject(requireFileRecord(objectKey));
    }

    @Override
    public SysFile requireFileRecord(String objectKey) {
        return findFileRecord(objectKey);
    }

    @Override
    public void assertImageFile(String objectKey) {
        SysFile record = sysFileMapper.selectOne(Wrappers.<SysFile>lambdaQuery()
                .eq(SysFile::getObjectKey, objectKey == null ? "" : objectKey.trim())
                .last("LIMIT 1"));
        String errorMessage = "图片文件不存在或类型不支持: " + objectKey;
        if (record == null || !isImageContentType(record.getContentType())) {
            throw new BusinessException(errorMessage);
        }
        validateImageSignature(record, errorMessage);
    }

    @Override
    public void assertAllowedFile(String objectKey) {
        requireFileRecord(objectKey);
    }

    private SysFile findFileRecord(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException("文件不存在");
        }
        String normalizedObjectKey = objectKey.trim();
        SysFile record = sysFileMapper.selectOne(Wrappers.<SysFile>lambdaQuery()
                .eq(SysFile::getObjectKey, normalizedObjectKey)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException("文件不存在");
        }
        return record;
    }

    private FileContentVO loadObject(SysFile record) {
        try {
            InputStream inputStream = openObject(record.getObjectKey());
            return new FileContentVO(
                    inputStream,
                    record.getOriginalName(),
                    normalizeContentType(record.getContentType()),
                    record.getSizeBytes()
            );
        } catch (Exception ex) {
            throw new BusinessException("文件读取失败，请确认 MinIO 服务已启动");
        }
    }

    private void validateImageSignature(SysFile record, String errorMessage) {
        byte[] header = new byte[IMAGE_SIGNATURE_LENGTH];
        int length = 0;
        try (InputStream inputStream = openObject(record.getObjectKey())) {
            while (length < header.length) {
                int read = inputStream.read(header, length, header.length - length);
                if (read < 0) {
                    break;
                }
                length += read;
            }
        } catch (Exception ex) {
            throw new BusinessException(errorMessage);
        }
        if (!matchesImageSignature(record.getContentType(), header, length)) {
            throw new BusinessException(errorMessage);
        }
    }

    private boolean matchesImageSignature(String contentType, byte[] header, int length) {
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/jpeg" -> length >= 3
                    && unsigned(header[0]) == 0xFF && unsigned(header[1]) == 0xD8 && unsigned(header[2]) == 0xFF;
            case "image/png" -> length >= 8
                    && unsigned(header[0]) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            case "image/webp" -> length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            case "image/gif" -> length >= 6
                    && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8'
                    && (header[4] == '7' || header[4] == '9') && header[5] == 'a';
            default -> false;
        };
    }

    private boolean isImageContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return switch (contentType.trim().toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/png", "image/webp", "image/gif" -> true;
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private InputStream openObject(String objectKey) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectKey)
                .build());
    }

    private String normalizeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.trim() : DEFAULT_CONTENT_TYPE;
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
    }
}