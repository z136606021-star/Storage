package com.storage.service;

import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.BomCatalogItemVO;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.ImportResultVO;
import com.storage.dto.MaterialQueryDTO;
import com.storage.dto.MaterialSaveDTO;
import com.storage.dto.PageResult;
import com.storage.entity.MaterialLedger;
import org.springframework.web.multipart.MultipartFile;

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

    MaterialLedger update(Long id, MaterialSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<MaterialLedger> listByQuery(MaterialQueryDTO query);

    byte[] export(MaterialQueryDTO query) throws IOException;

    byte[] exportTemplate() throws IOException;

    ImportResultVO importExcel(MultipartFile file) throws IOException;

    List<String> listBinCodes();

    List<BomCatalogItemVO> listBomCatalog();

    FilterOptionsVO filterOptions(FilterLinkageQueryDTO query);
}
