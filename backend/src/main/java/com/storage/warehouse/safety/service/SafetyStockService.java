package com.storage.warehouse.safety.service;

import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.safety.dto.SafetyStockQueryDTO;
import com.storage.warehouse.safety.dto.SafetyStockRecordVO;
import com.storage.warehouse.safety.dto.SafetyStockUpdateDTO;

import java.io.IOException;
import java.util.List;

public interface SafetyStockService {

    PageResult<SafetyStockRecordVO> page(SafetyStockQueryDTO query);

    SafetyStockRecordVO getByMaterialLedgerId(Long materialLedgerId);

    SafetyStockRecordVO upsert(Long materialLedgerId, SafetyStockUpdateDTO dto);

    FilterOptionsVO filterOptions(FilterLinkageQueryDTO query);

    byte[] export(SafetyStockQueryDTO query) throws IOException;

    List<SafetyStockRecordVO> listByQuery(SafetyStockQueryDTO query);
}
