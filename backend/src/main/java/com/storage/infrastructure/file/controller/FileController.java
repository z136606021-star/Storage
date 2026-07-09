package com.storage.infrastructure.file.controller;

import com.storage.infrastructure.file.dto.FileUploadVO;
import com.storage.infrastructure.file.service.FileStorageService;
import com.storage.system.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final AuthService authService;

    @PostMapping("/upload")
    @RequiresPermissions(value = {"platform:file:upload", "warehouse:bom:write", "platform:experience:write"}, logical = Logical.OR)
    public FileUploadVO upload(@RequestParam("file") MultipartFile file) {
        var user = authService.currentUser();
        Long uploaderId = user == null ? null : user.getId();
        return fileStorageService.upload(file, uploaderId);
    }

    @GetMapping("/preview")
    public ResponseEntity<StreamingResponseBody> preview(@RequestParam("objectKey") String objectKey) {
        var file = fileStorageService.loadImage(objectKey);
        StreamingResponseBody body = outputStream -> {
            try (var inputStream = file.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        String filename = file.originalName() == null ? "image" : file.originalName();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        var response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename);
        if (file.sizeBytes() != null && file.sizeBytes() >= 0) {
            response.contentLength(file.sizeBytes());
        }
        return response.body(body);
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam("objectKey") String objectKey) {
        var file = fileStorageService.loadFile(objectKey);
        StreamingResponseBody body = outputStream -> {
            try (var inputStream = file.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        String filename = file.originalName() == null ? "file" : file.originalName();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        var response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        if (file.sizeBytes() != null && file.sizeBytes() >= 0) {
            response.contentLength(file.sizeBytes());
        }
        return response.body(body);
    }
}
