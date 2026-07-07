package com.storage.infrastructure.file.service;

import com.storage.infrastructure.file.config.FileUploadProperties;
import com.storage.infrastructure.file.config.MinioProperties;
import com.storage.common.exception.BusinessException;
import com.storage.infrastructure.file.mapper.SysFileMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private SysFileMapper sysFileMapper;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        MinioProperties minioProperties = new MinioProperties();
        minioProperties.setBucket("storage");

        FileUploadProperties fileUploadProperties = new FileUploadProperties();
        fileUploadProperties.setMaxSizeBytes(5 * 1024 * 1024L);
        fileUploadProperties.setAllowedContentTypes(List.of(
                "image/jpeg",
                "image/png",
                "image/webp",
                "image/gif"
        ));

        fileStorageService = new FileStorageServiceImpl(
                minioClient,
                minioProperties,
                fileUploadProperties,
                sysFileMapper
        );
    }

    @Test
    void upload_withOversizedFile_rejectsBeforeMinioCall() {
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                content
        );

        assertThatThrownBy(() -> fileStorageService.upload(file, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件大小超过限制");
        verifyNoInteractions(minioClient, sysFileMapper);
    }

    @Test
    void upload_withUnsupportedContentType_rejectsBeforeMinioCall() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "demo".getBytes()
        );

        assertThatThrownBy(() -> fileStorageService.upload(file, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件类型不支持");
        verifyNoInteractions(minioClient, sysFileMapper);
    }

    @Test
    void upload_withForgedImageContentType_rejectsBeforeMinioCall() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "not a real png".getBytes()
        );

        assertThatThrownBy(() -> fileStorageService.upload(file, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件内容与类型不匹配");
        verifyNoInteractions(minioClient, sysFileMapper);
    }
}
