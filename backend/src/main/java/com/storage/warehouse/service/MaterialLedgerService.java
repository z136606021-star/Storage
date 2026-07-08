package com.storage.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.dto.MaterialQueryDTO;
import com.storage.warehouse.dto.MaterialSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.dto.BomCatalogItemVO;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;

import java.io.IOException;
import java.util.List;

public interface MaterialLedgerService extends IService<MaterialLedger> {

    PageResult<MaterialLedger> page(MaterialQueryDTO query);

    MaterialLedger findByMaterialKey(
            String category,
            String genericName,
            String brand,
            String name,
            String model,
            String binLocation
    );

    MaterialLedger create(MaterialSaveDTO dto);

    List<MaterialLedger> listByQuery(MaterialQueryDTO query);

    byte[] export(MaterialQueryDTO query) throws IOException;

    List<String> listBinCodes();

    List<BomCatalogItemVO> listBomCatalog();

    FilterOptionsVO filterOptions(FilterLinkageQueryDTO query);
}
