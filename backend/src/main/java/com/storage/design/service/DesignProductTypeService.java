package com.storage.design.service;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.design.dto.DesignProductTypeQueryDTO;
import com.storage.design.dto.DesignProductTypeSaveDTO;
import com.storage.design.entity.DesignProductType;

import java.io.IOException;
import java.util.List;

public interface DesignProductTypeService {

    PageResult<DesignProductType> page(DesignProductTypeQueryDTO query);

    DesignProductType getById(Long id);

    DesignProductType create(DesignProductTypeSaveDTO dto);

    DesignProductType update(Long id, DesignProductTypeSaveDTO dto);

    void delete(Long id);

    void batchDelete(BatchDeleteDTO dto);

    List<DesignProductType> listByQuery(DesignProductTypeQueryDTO query);

    List<DesignProductType> listEnabled();

    DesignProductType requireEnabled(Long id);

    DesignProductType findEnabledByCodeOrName(String typeCode, String typeName);

    byte[] export(DesignProductTypeQueryDTO query) throws IOException;
}
