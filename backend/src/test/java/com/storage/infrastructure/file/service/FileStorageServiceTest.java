package com.storage.infrastructure.file.service;

import com.storage.common.exception.BusinessException;
import com.storage.infrastructure.file.config.FileUploadProperties;
import com.storage.infrastructure.file.config.MinioProperties;
import com.storage.infrastructure.file.entity.SysFile;
import com.storage.infrastructure.file.mapper.SysFileMapper;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private static final byte[] PNG_HEADER = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

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
        fileUploadProperties.setMaxSizeBytes(5_505_025_024L);

        fileStorageService = new FileStorageServiceImpl(
                minioClient,
                minioProperties,
                fileUploadProperties,
                sysFileMapper
        );
    }

    @Test
    void uploadPolicy_returnsConfiguredLimits() {
        var policy = fileStorageService.uploadPolicy();

        assertThat(policy.getMaxSizeBytes()).isEqualTo(5_505_025_024L);
    }

    @Test
    void assertImageFile_rejectsNonImageRecord() {
        SysFile record = imageRecord("2026-07-08/demo.txt", "text/plain");
        when(sysFileMapper.selectOne(any())).thenReturn(record);

        assertThatThrownBy(() -> fileStorageService.assertImageFile(record.getObjectKey()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片文件不存在或类型不支持");
        verifyNoInteractions(minioClient);
    }

    @Test
    void assertImageFile_rejectsForgedPngContent() throws Exception {
        SysFile record = imageRecord("2026-07-08/forged.png", "image/png");
        when(sysFileMapper.selectOne(any())).thenReturn(record);
        when(minioClient.getObject(any())).thenReturn(getObjectResponse("not a png".getBytes()));

        assertThatThrownBy(() -> fileStorageService.assertImageFile(record.getObjectKey()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片文件不存在或类型不支持");
    }

    @Test
    void assertImageFile_acceptsRealPngHeader() throws Exception {
        SysFile record = imageRecord("2026-07-08/demo.png", "image/png");
        when(sysFileMapper.selectOne(any())).thenReturn(record);
        when(minioClient.getObject(any())).thenReturn(getObjectResponse(PNG_HEADER));

        fileStorageService.assertImageFile(record.getObjectKey());

        verify(minioClient).getObject(any());
    }

    @Test
    void loadImage_rejectsForgedPngContent() throws Exception {
        SysFile record = imageRecord("2026-07-08/forged.png", "image/png");
        when(sysFileMapper.selectOne(any())).thenReturn(record);
        when(minioClient.getObject(any())).thenReturn(getObjectResponse("not a png".getBytes()));

        assertThatThrownBy(() -> fileStorageService.loadImage(record.getObjectKey()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件类型不支持预览");
    }

    @Test
    void loadImage_acceptsRealPngHeaderAndReturnsFreshStream() throws Exception {
        SysFile record = imageRecord("2026-07-08/demo.png", "image/png");
        record.setOriginalName("demo.png");
        record.setSizeBytes((long) PNG_HEADER.length);
        when(sysFileMapper.selectOne(any())).thenReturn(record);
        when(minioClient.getObject(any())).thenAnswer(invocation -> getObjectResponse(PNG_HEADER));

        var result = fileStorageService.loadImage(record.getObjectKey());

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.inputStream().readAllBytes()).containsExactly(PNG_HEADER);
        verify(minioClient, org.mockito.Mockito.times(2)).getObject(any());
    }

    @Test
    void resolveAccessUrl_returnsSameOriginPreviewUrl() {
        String url = fileStorageService.resolveAccessUrl("2026-07-08/demo image.png");

        assertThat(url).isEqualTo("/api/files/preview?objectKey=2026-07-08%2Fdemo+image.png");
    }

    @Test
    void loadImage_withNonImageFile_rejectsBeforeMinioCall() {
        SysFile record = imageRecord("2026-07-08/demo.txt", "text/plain");
        when(sysFileMapper.selectOne(any())).thenReturn(record);

        assertThatThrownBy(() -> fileStorageService.loadImage(record.getObjectKey()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件类型不支持预览");
        verifyNoInteractions(minioClient);
    }

    @Test
    void upload_withOversizedFile_rejectsBeforeMinioCall() {
        FileUploadProperties smallLimitProperties = new FileUploadProperties();
        smallLimitProperties.setMaxSizeBytes(1024);
        MinioProperties minioProperties = new MinioProperties();
        minioProperties.setBucket("storage");
        FileStorageService smallLimitService = new FileStorageServiceImpl(
                minioClient,
                minioProperties,
                smallLimitProperties,
                sysFileMapper
        );

        byte[] content = new byte[1025];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.bin",
                "application/octet-stream",
                content
        );

        assertThatThrownBy(() -> smallLimitService.upload(file, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件大小超过限制");
        verifyNoInteractions(minioClient, sysFileMapper);
    }

    @Test
    void upload_withArbitraryContentType_remainsAllowed() throws Exception {
        when(minioClient.putObject(any())).thenReturn(mock(io.minio.ObjectWriteResponse.class));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "archive.custom",
                "application/x-custom-binary",
                "demo".getBytes()
        );

        var result = fileStorageService.upload(file, 1L);

        ArgumentCaptor<PutObjectArgs> putArgs = ArgumentCaptor.forClass(PutObjectArgs.class);
        ArgumentCaptor<SysFile> record = ArgumentCaptor.forClass(SysFile.class);
        verify(minioClient).putObject(putArgs.capture());
        verify(sysFileMapper).insert(record.capture());
        assertThat(putArgs.getValue().contentType()).isEqualTo("application/x-custom-binary");
        assertThat(record.getValue().getContentType()).isEqualTo("application/x-custom-binary");
        assertThat(result.getContentType()).isEqualTo("application/x-custom-binary");
    }

    @Test
    void upload_withNullContentType_normalizesEveryStoredValue() throws Exception {
        when(minioClient.putObject(any())).thenReturn(mock(io.minio.ObjectWriteResponse.class));
        MockMultipartFile file = new MockMultipartFile("file", "app.bin", null, "demo".getBytes());

        var result = fileStorageService.upload(file, null);

        ArgumentCaptor<PutObjectArgs> putArgs = ArgumentCaptor.forClass(PutObjectArgs.class);
        ArgumentCaptor<SysFile> record = ArgumentCaptor.forClass(SysFile.class);
        verify(minioClient).putObject(putArgs.capture());
        verify(sysFileMapper).insert(record.capture());
        assertThat(putArgs.getValue().contentType()).isEqualTo("application/octet-stream");
        assertThat(record.getValue().getContentType()).isEqualTo("application/octet-stream");
        assertThat(result.getContentType()).isEqualTo("application/octet-stream");
        assertThat(result.getOriginalName()).isEqualTo("app.bin");
    }

    private SysFile imageRecord(String objectKey, String contentType) {
        SysFile record = new SysFile();
        record.setObjectKey(objectKey);
        record.setContentType(contentType);
        return record;
    }

    private GetObjectResponse getObjectResponse(byte[] content) {
        return new GetObjectResponse(
                Headers.of(),
                "storage",
                null,
                "object-key",
                new ByteArrayInputStream(content)
        );
    }
}