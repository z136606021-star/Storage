package com.storage.design.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.ImportResultVO;
import com.storage.common.dto.PageResult;
import com.storage.design.dto.DesignGuideFilterOptionsVO;
import com.storage.design.dto.DesignGuideQueryDTO;
import com.storage.design.dto.DesignGuideSaveDTO;
import com.storage.design.entity.DesignGuide;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DesignGuideService {

    PageResult<DesignGuide> page(DesignGuideQueryDTO query);

    DesignGuide getById(Long id);

    DesignGuide create(DesignGuideSaveDTO dto);

    DesignGuide update(Long id, DesignGuideSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<DesignGuide> listByQuery(DesignGuideQueryDTO query);

    DesignGuideFilterOptionsVO filterOptions();

    byte[] export(DesignGuideQueryDTO query) throws IOException;

    byte[] exportTemplate() throws IOException;

    ImportResultVO importExcel(MultipartFile file) throws IOException;
}
