package com.storage.warehouse.io.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.query.PageSupport;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import com.storage.warehouse.io.converter.MaterialIoConverter;
import com.storage.warehouse.io.dto.MaterialIoQueryDTO;
import com.storage.warehouse.io.dto.MaterialIoRecordVO;
import com.storage.warehouse.io.dto.MaterialIoSafetyHintVO;
import com.storage.warehouse.io.entity.MaterialIoRecord;
import com.storage.warehouse.io.exception.MaterialIoNotFoundException;
import com.storage.warehouse.io.mapper.MaterialIoRecordMapper;
import com.storage.warehouse.io.query.MaterialIoQueryBuilder;
import com.storage.warehouse.ledger.dto.MaterialQueryDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import com.storage.warehouse.ledger.query.MaterialLedgerQueryBuilder;
import com.storage.warehouse.ledger.service.MaterialLedgerService;
import com.storage.warehouse.safety.entity.SafetyStock;
import com.storage.warehouse.safety.mapper.SafetyStockMapper;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialIoReadService {

    private final MaterialIoRecordMapper materialIoRecordMapper;
    private final MaterialLedgerMapper materialLedgerMapper;
    private final SafetyStockMapper safetyStockMapper;
    private final OperatorResolver operatorResolver;
    private final MaterialIoConverter materialIoConverter;
    private final MaterialLedgerService materialLedgerService;

    public PageResult<MaterialIoRecordVO> page(MaterialIoQueryDTO query) {
        PageSupport.PageSpec pageSpec = PageSupport.normalize(query.getPage(), query.getPageSize());

        Collection<Long> ledgerIds = resolveMaterialLedgerIds(query);
        if (ledgerIds != null && ledgerIds.isEmpty()) {
            return PageSupport.empty(pageSpec);
        }

        var result = materialIoRecordMapper.selectPage(
                PageSupport.page(pageSpec.page(), pageSpec.pageSize()),
                MaterialIoQueryBuilder.build(query, ledgerIds)
        );
        return PageSupport.result(result, toVoList(result.getRecords()));
    }

    public MaterialIoRecordVO getById(Long id) {
        MaterialIoRecord record = requireRecord(id);
        MaterialLedger ledger = materialLedgerMapper.selectById(record.getMaterialLedgerId());
        OperatorInfo operator = operatorResolver.findById(record.getOperatorUserId());
        return materialIoConverter.toVo(record, ledger, operator);
    }

    public List<MaterialIoRecordVO> listByQuery(MaterialIoQueryDTO query) {
        Collection<Long> ledgerIds = resolveMaterialLedgerIds(query);
        if (ledgerIds != null && ledgerIds.isEmpty()) {
            return List.of();
        }
        List<MaterialIoRecord> records = materialIoRecordMapper.selectList(
                MaterialIoQueryBuilder.build(query, ledgerIds)
        );
        return toVoList(records);
    }

    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return materialLedgerService.filterOptions(query);
    }

    public List<MaterialIoSafetyHintVO> safetyHints(List<Long> materialLedgerIds) {
        if (CollectionUtils.isEmpty(materialLedgerIds)) {
            return List.of();
        }

        List<Long> ids = materialLedgerIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, MaterialLedger> ledgerMap = materialLedgerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MaterialLedger::getId, Function.identity()));

        Map<Long, SafetyStock> safetyMap = safetyStockMapper.selectList(
                Wrappers.<SafetyStock>lambdaQuery().in(SafetyStock::getMaterialLedgerId, ids)
        ).stream().collect(Collectors.toMap(SafetyStock::getMaterialLedgerId, Function.identity(), (a, b) -> a));

        List<MaterialIoSafetyHintVO> hints = new ArrayList<>();
        for (Long ledgerId : ids) {
            MaterialIoSafetyHintVO hint = new MaterialIoSafetyHintVO();
            hint.setMaterialLedgerId(ledgerId);
            MaterialLedger ledger = ledgerMap.get(ledgerId);
            if (ledger != null) {
                hint.setCurrentStock(ledger.getStockQuantity());
            }
            SafetyStock safetyStock = safetyMap.get(ledgerId);
            if (safetyStock != null) {
                hint.setSafetyQuantity(safetyStock.getSafetyQuantity());
                hint.setWarningEnabled(safetyStock.getWarningEnabled());
            }
            hints.add(hint);
        }
        return hints;
    }

    private Collection<Long> resolveMaterialLedgerIds(MaterialIoQueryDTO query) {
        Set<Long> result = null;

        if (query.getMaterialLedgerId() != null) {
            result = new HashSet<>();
            result.add(query.getMaterialLedgerId());
        }

        if (MaterialIoQueryBuilder.hasMaterialFilters(query)) {
            MaterialQueryDTO materialQuery = new MaterialQueryDTO();
            materialQuery.setCategory(query.getCategory());
            materialQuery.setGenericName(query.getGenericName());
            materialQuery.setBrand(query.getBrand());
            materialQuery.setName(query.getName());
            materialQuery.setModel(query.getModel());
            materialQuery.setBinLocation(query.getBinLocation());
            Set<Long> filtered = materialLedgerMapper.selectList(MaterialLedgerQueryBuilder.build(materialQuery)).stream()
                    .map(MaterialLedger::getId)
                    .collect(Collectors.toSet());
            if (result == null) {
                return filtered;
            }
            result.retainAll(filtered);
            return result;
        }

        return result;
    }

    private List<MaterialIoRecordVO> toVoList(List<MaterialIoRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return List.of();
        }

        Set<Long> ledgerIds = records.stream()
                .map(MaterialIoRecord::getMaterialLedgerId)
                .collect(Collectors.toSet());
        Map<Long, MaterialLedger> ledgerMap = materialLedgerMapper.selectBatchIds(ledgerIds).stream()
                .collect(Collectors.toMap(MaterialLedger::getId, Function.identity()));

        Set<Long> operatorIds = records.stream()
                .map(MaterialIoRecord::getOperatorUserId)
                .collect(Collectors.toSet());
        Map<Long, OperatorInfo> operatorMap = operatorResolver.findByIds(operatorIds);

        return records.stream()
                .map(record -> materialIoConverter.toVo(
                        record,
                        ledgerMap.get(record.getMaterialLedgerId()),
                        operatorMap.get(record.getOperatorUserId())
                ))
                .collect(Collectors.toList());
    }

    private MaterialIoRecord requireRecord(Long id) {
        MaterialIoRecord record = materialIoRecordMapper.selectById(id);
        if (record == null) {
            throw new MaterialIoNotFoundException(id);
        }
        return record;
    }
}
