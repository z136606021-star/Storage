package com.storage.controller;

import com.storage.dto.FileUploadVO;
import com.storage.service.AuthService;
import com.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final AuthService authService;

    @PostMapping("/upload")
    @RequiresPermissions("platform:file:upload")
    public FileUploadVO upload(@RequestParam("file") MultipartFile file) {
        return fileStorageService.upload(file, authService.currentUser());
    }
}
