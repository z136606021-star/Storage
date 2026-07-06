package com.storage.warehouse.io.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.io.dto.MaterialIoBatchSaveDTO;
import com.storage.warehouse.io.dto.MaterialIoQueryDTO;
import com.storage.warehouse.io.dto.MaterialIoRecordVO;
import com.storage.warehouse.io.dto.MaterialIoSafetyHintVO;
import com.storage.warehouse.io.dto.MaterialIoSaveDTO;
import com.storage.warehouse.io.dto.MaterialIoUpdateDTO;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;

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
