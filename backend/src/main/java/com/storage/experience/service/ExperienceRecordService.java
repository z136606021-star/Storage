package com.storage.experience.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.experience.dto.ExperienceFilterOptionsVO;
import com.storage.experience.dto.ExperienceRecordDetailVO;
import com.storage.experience.dto.ExperienceRecordQueryDTO;
import com.storage.experience.dto.ExperienceRecordSaveDTO;
import com.storage.experience.dto.ExperienceRecordVO;

import java.io.IOException;
import java.util.List;

public interface ExperienceRecordService {

    PageResult<ExperienceRecordVO> page(ExperienceRecordQueryDTO query);

    ExperienceRecordDetailVO getById(Long id);

    ExperienceRecordDetailVO create(ExperienceRecordSaveDTO dto);

    ExperienceRecordDetailVO createImported(ExperienceRecordSaveDTO dto, String recorderName);

    ExperienceRecordDetailVO update(Long id, ExperienceRecordSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<ExperienceRecordDetailVO> listDetailsByQuery(ExperienceRecordQueryDTO query);

    ExperienceFilterOptionsVO filterOptions();

    byte[] export(ExperienceRecordQueryDTO query) throws IOException;

    byte[] exportTemplate() throws IOException;
}
