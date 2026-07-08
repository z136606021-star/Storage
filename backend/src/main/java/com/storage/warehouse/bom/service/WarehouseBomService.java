package com.storage.warehouse.bom.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.shared.dto.BomCatalogItemVO;
import com.storage.warehouse.shared.dto.BomFilterOptionsVO;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import com.storage.warehouse.bom.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.bom.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.bom.entity.WarehouseBom;

import java.io.IOException;
import java.util.List;

public interface WarehouseBomService {

    PageResult<WarehouseBom> page(WarehouseBomQueryDTO query);

    WarehouseBom getById(Long id);

    boolean existsByCatalogKey(String category, String genericName, String brand, String name, String model);

    void assertCatalogExists(String category, String genericName, String brand, String name, String model);

    List<BomCatalogItemVO> listCatalogSummaries();

    WarehouseBom create(WarehouseBomSaveDTO dto);

    WarehouseBom update(Long id, WarehouseBomSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<WarehouseBom> listByQuery(WarehouseBomQueryDTO query);

    byte[] export(WarehouseBomQueryDTO query) throws IOException;

    byte[] exportTemplate() throws IOException;

    BomFilterOptionsVO filterOptions(FilterLinkageQueryDTO query);
}
