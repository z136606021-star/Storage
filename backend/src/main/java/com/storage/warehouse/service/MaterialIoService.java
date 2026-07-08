package com.storage.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.dto.MaterialIoBatchSaveDTO;
import com.storage.warehouse.dto.MaterialIoQueryDTO;
import com.storage.warehouse.dto.MaterialIoRecordVO;
import com.storage.warehouse.dto.MaterialIoSafetyHintVO;
import com.storage.warehouse.dto.MaterialIoSaveDTO;
import com.storage.warehouse.dto.MaterialIoUpdateDTO;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import com.storage.warehouse.entity.MaterialIoRecord;

import java.util.List;

public interface MaterialIoService extends IService<MaterialIoRecord> {

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
