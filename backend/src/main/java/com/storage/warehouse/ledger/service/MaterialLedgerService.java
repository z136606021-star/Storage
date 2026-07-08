package com.storage.warehouse.ledger.service;

import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.ledger.dto.MaterialQueryDTO;
import com.storage.warehouse.ledger.dto.MaterialSaveDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.shared.dto.BomCatalogItemVO;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;

import java.io.IOException;
import java.util.List;

public interface MaterialLedgerService {

    PageResult<MaterialLedger> page(MaterialQueryDTO query);

    MaterialLedger getById(Long id);

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
