package com.storage.service;

import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.MaterialIoBatchSaveDTO;
import com.storage.dto.MaterialIoQueryDTO;
import com.storage.dto.MaterialIoRecordVO;
import com.storage.dto.MaterialIoSafetyHintVO;
import com.storage.dto.MaterialIoSaveDTO;
import com.storage.dto.MaterialIoUpdateDTO;
import com.storage.dto.PageResult;

import java.util.List;

public interface MaterialIoService {

    PageResult<MaterialIoRecordVO> page(MaterialIoQueryDTO query);

    MaterialIoRecordVO getById(Long id);

    List<MaterialIoRecordVO> listByQuery(MaterialIoQueryDTO query);

    FilterOptionsVO filterOptions(FilterLinkageQueryDTO query);

    List<MaterialIoSafetyHintVO> safetyHints(List<Long> materialLedgerIds);

    int importBatch(List<MaterialIoSaveDTO> dtos);

    List<MaterialIoRecordVO> batchCreate(MaterialIoBatchSaveDTO dto);

    MaterialIoRecordVO update(Long id, MaterialIoUpdateDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);
}
