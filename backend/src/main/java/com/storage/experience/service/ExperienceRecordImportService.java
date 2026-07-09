package com.storage.experience.service;

import com.storage.common.dto.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ExperienceRecordImportService {

    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
