package com.storage.design.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.design.dto.DesignStageQueryDTO;
import com.storage.design.dto.DesignStageSaveDTO;
import com.storage.design.entity.DesignStage;

import java.io.IOException;
import java.util.List;

public interface DesignStageService {

    PageResult<DesignStage> page(DesignStageQueryDTO query);

    DesignStage getById(Long id);

    DesignStage create(DesignStageSaveDTO dto);

    DesignStage update(Long id, DesignStageSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<DesignStage> listByQuery(DesignStageQueryDTO query);

    List<DesignStage> listEnabled();

    DesignStage requireEnabled(Long id);

    DesignStage findEnabledByName(String stageName);

    byte[] export(DesignStageQueryDTO query) throws IOException;
}
