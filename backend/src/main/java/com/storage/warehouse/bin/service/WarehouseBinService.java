package com.storage.warehouse.bin.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.bin.dto.WarehouseBinQueryDTO;
import com.storage.warehouse.bin.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.bin.entity.WarehouseBin;

import java.io.IOException;
import java.util.List;

public interface WarehouseBinService {

    PageResult<WarehouseBin> page(WarehouseBinQueryDTO query);

    WarehouseBin getById(Long id);

    List<String> listAllCodes();

    boolean existsByBinCode(String binCode);

    void assertBinExists(String binCode);

    WarehouseBin create(WarehouseBinSaveDTO dto);

    WarehouseBin update(Long id, WarehouseBinSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<WarehouseBin> listByQuery(WarehouseBinQueryDTO query);

    byte[] export(WarehouseBinQueryDTO query) throws IOException;

    byte[] exportTemplate() throws IOException;
}
