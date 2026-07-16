package com.storage.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.dto.BomCatalogItemVO;
import com.storage.warehouse.dto.BomFilterOptionsVO;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import com.storage.warehouse.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.entity.WarehouseBom;

import java.io.IOException;
import java.util.List;

public interface WarehouseBomService extends IService<WarehouseBom> {

    PageResult<WarehouseBom> page(WarehouseBomQueryDTO query);

    WarehouseBom getById(Long id);

    boolean existsByCatalogKey(String category, String genericName, String brand, String name);

    void assertCatalogExists(String category, String genericName, String brand, String name);

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
