package com.storage.service;

import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.PageResult;
import com.storage.dto.SafetyStockQueryDTO;
import com.storage.dto.SafetyStockRecordVO;
import com.storage.dto.SafetyStockUpdateDTO;

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
