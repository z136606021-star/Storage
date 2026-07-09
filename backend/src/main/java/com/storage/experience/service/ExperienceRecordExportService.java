package com.storage.experience.service;

import com.storage.experience.dto.ExperienceRecordDetailVO;

import java.io.IOException;
import java.util.List;

public interface ExperienceRecordExportService {

    byte[] export(List<ExperienceRecordDetailVO> records) throws IOException;

    byte[] exportTemplate() throws IOException;
}
