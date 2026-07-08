package com.storage.warehouse.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.query.PageSupport;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.exception.MaterialLedgerNotFoundException;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.service.MaterialLedgerService;
import com.storage.warehouse.converter.SafetyStockConverter;
import com.storage.warehouse.dto.SafetyStockQueryDTO;
import com.storage.warehouse.dto.SafetyStockRecordVO;
import com.storage.warehouse.dto.SafetyStockUpdateDTO;
import com.storage.warehouse.entity.SafetyStock;
import com.storage.warehouse.mapper.SafetyStockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SafetyStockServiceImpl extends ServiceImpl<SafetyStockMapper, SafetyStock>
        implements SafetyStockService {

    private final SafetyStockMapper safetyStockMapper;
    private final MaterialLedgerMapper materialLedgerMapper;
    private final MaterialLedgerService materialLedgerService;
    private final SafetyStockConverter safetyStockConverter;
    private final SafetyStockExportService safetyStockExportService;

    @Override
    public PageResult<SafetyStockRecordVO> page(SafetyStockQueryDTO query) {
        var pageRequest = PageSupport.<SafetyStockRecordVO>page(query.getPage(), query.getPageSize());
        var result = safetyStockMapper.selectJoinedPage(pageRequest, query);
        enrichRecords(result.getRecords());
        return PageSupport.result(result);
    }

    @Override
    public SafetyStockRecordVO getByMaterialLedgerId(Long materialLedgerId) {
        MaterialLedger ledger = requireLedger(materialLedgerId);
        SafetyStock safetyStock = findByLedgerId(materialLedgerId);
        return safetyStockConverter.toVo(ledger, safetyStock);
    }

    @Override
    @Transactional
    public SafetyStockRecordVO upsert(Long materialLedgerId, SafetyStockUpdateDTO dto) {
        MaterialLedger ledger = requireLedger(materialLedgerId);
        SafetyStock existing = findByLedgerId(materialLedgerId);

        if (existing == null) {
            SafetyStock created = new SafetyStock();
            created.setMaterialLedgerId(materialLedgerId);
            created.setSafetyQuantity(dto.getSafetyQuantity());
            created.setWarningEnabled(dto.getWarningEnabled());
            save(created);
            return safetyStockConverter.toVo(ledger, created);
        }

        existing.setSafetyQuantity(dto.getSafetyQuantity());
        existing.setWarningEnabled(dto.getWarningEnabled());
        updateById(existing);
        return safetyStockConverter.toVo(ledger, existing);
    }

    @Override
    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return materialLedgerService.filterOptions(query);
    }

    @Override
    public byte[] export(SafetyStockQueryDTO query) throws IOException {
        List<SafetyStockRecordVO> records = listByQuery(query);
        return safetyStockExportService.export(records);
    }

    @Override
    public byte[] exportPurchaseList(SafetyStockQueryDTO query) throws IOException {
        SafetyStockQueryDTO warningQuery = copyQuery(query);
        warningQuery.setWarningPeriod("是");
        return safetyStockExportService.exportPurchaseList(listByQuery(warningQuery));
    }

    @Override
    public List<SafetyStockRecordVO> listByQuery(SafetyStockQueryDTO query) {
        List<SafetyStockRecordVO> records = safetyStockMapper.selectJoinedList(query);
        enrichRecords(records);
        return records;
    }

    private void enrichRecords(List<SafetyStockRecordVO> records) {
        records.forEach(safetyStockConverter::enrichWarningPeriod);
    }

    private MaterialLedger requireLedger(Long materialLedgerId) {
        MaterialLedger ledger = materialLedgerMapper.selectById(materialLedgerId);
        if (ledger == null) {
            throw new MaterialLedgerNotFoundException(materialLedgerId);
        }
        return ledger;
    }

    private SafetyStock findByLedgerId(Long materialLedgerId) {
        return getOne(
                Wrappers.<SafetyStock>lambdaQuery()
                        .eq(SafetyStock::getMaterialLedgerId, materialLedgerId)
                        .last("LIMIT 1")
        );
    }

    private SafetyStockQueryDTO copyQuery(SafetyStockQueryDTO source) {
        SafetyStockQueryDTO target = new SafetyStockQueryDTO();
        if (source == null) {
            return target;
        }
        target.setIds(source.getIds());
        target.setPage(source.getPage());
        target.setPageSize(source.getPageSize());
        target.setCategory(source.getCategory());
        target.setGenericName(source.getGenericName());
        target.setBrand(source.getBrand());
        target.setName(source.getName());
        target.setModel(source.getModel());
        target.setBinLocation(source.getBinLocation());
        target.setSafetyQuantityKeyword(source.getSafetyQuantityKeyword());
        target.setWarningPeriod(source.getWarningPeriod());
        return target;
    }
}
