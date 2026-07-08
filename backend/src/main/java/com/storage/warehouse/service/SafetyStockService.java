package com.storage.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.warehouse.dto.SafetyStockQueryDTO;
import com.storage.warehouse.dto.SafetyStockRecordVO;
import com.storage.warehouse.dto.SafetyStockUpdateDTO;
import com.storage.warehouse.entity.SafetyStock;

import java.io.IOException;
import java.util.List;

public interface SafetyStockService extends IService<SafetyStock> {

    PageResult<SafetyStockRecordVO> page(SafetyStockQueryDTO query);

    SafetyStockRecordVO getByMaterialLedgerId(Long materialLedgerId);

    SafetyStockRecordVO upsert(Long materialLedgerId, SafetyStockUpdateDTO dto);

    FilterOptionsVO filterOptions(FilterLinkageQueryDTO query);

    byte[] export(SafetyStockQueryDTO query) throws IOException;

    byte[] exportPurchaseList(SafetyStockQueryDTO query) throws IOException;

    List<SafetyStockRecordVO> listByQuery(SafetyStockQueryDTO query);
}
