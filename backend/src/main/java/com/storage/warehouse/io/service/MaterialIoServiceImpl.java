package com.storage.warehouse.io.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import com.storage.warehouse.io.converter.MaterialIoConverter;
import com.storage.warehouse.io.dto.MaterialIoBatchItemDTO;
import com.storage.warehouse.io.dto.MaterialIoBatchSaveDTO;
import com.storage.warehouse.io.dto.MaterialIoQueryDTO;
import com.storage.warehouse.io.dto.MaterialIoRecordVO;
import com.storage.warehouse.io.dto.MaterialIoSafetyHintVO;
import com.storage.warehouse.io.dto.MaterialIoSaveDTO;
import com.storage.warehouse.io.dto.MaterialIoUpdateDTO;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
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
public class MaterialIoServiceImpl implements MaterialIoService {

    private final MaterialIoRecordMapper materialIoRecordMapper;
    private final MaterialLedgerMapper materialLedgerMapper;
    private final SafetyStockMapper safetyStockMapper;
    private final OperatorResolver operatorResolver;
    private final MaterialIoConverter materialIoConverter;
    private final MaterialLedgerService materialLedgerService;
    private final MaterialStockMutationService materialStockMutationService;

    @Override
    public PageResult<MaterialIoRecordVO> page(MaterialIoQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Collection<Long> ledgerIds = resolveMaterialLedgerIds(query);
        if (ledgerIds != null && ledgerIds.isEmpty()) {
            return new PageResult<>(List.of(), 0L, (long) page, (long) pageSize);
        }

        Page<MaterialIoRecord> result = materialIoRecordMapper.selectPage(
                new Page<>(page, pageSize),
                MaterialIoQueryBuilder.build(query, ledgerIds)
        );
        List<MaterialIoRecordVO> records = toVoList(result.getRecords());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public MaterialIoRecordVO getById(Long id) {
        MaterialIoRecord record = requireRecord(id);
        MaterialLedger ledger = materialLedgerMapper.selectById(record.getMaterialLedgerId());
        OperatorInfo operator = operatorResolver.findById(record.getOperatorUserId());
        return materialIoConverter.toVo(record, ledger, operator);
    }

    @Override
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

    @Override
    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return materialLedgerService.filterOptions(query);
    }

    @Override
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

    @Override
    @Transactional
    public int importBatch(List<MaterialIoSaveDTO> dtos) {
        if (CollectionUtils.isEmpty(dtos)) {
            return 0;
        }

        List<Long> ledgerIds = dtos.stream()
                .map(MaterialIoSaveDTO::getMaterialLedgerId)
                .filter(Objects::nonNull)
                .toList();
        assertNoDuplicateLedgerIds(ledgerIds);

        OperatorInfo operator = operatorResolver.requireCurrentOperator();

        List<Long> distinctLedgerIds = ledgerIds.stream().distinct().toList();
        Map<Long, MaterialLedger> lockedLedgers = materialStockMutationService.lockLedgersInOrder(distinctLedgerIds);

        for (MaterialIoSaveDTO dto : dtos) {
            LocalDateTime operatedAt = resolveOperatedAt(dto.getOperatedAt(), lockedLedgers.get(dto.getMaterialLedgerId()));
            createRecord(dto, operator.getId(), operatedAt, lockedLedgers);
        }
        return dtos.size();
    }

    @Override
    @Transactional
    public List<MaterialIoRecordVO> batchCreate(MaterialIoBatchSaveDTO dto) {
        String ioType = MaterialIoQueryBuilder.normalizeIoType(dto.getIoType());
        MaterialIoQueryBuilder.assertValidIoType(ioType);

        List<Long> itemLedgerIds = dto.getItems().stream()
                .map(MaterialIoBatchItemDTO::getMaterialLedgerId)
                .toList();
        assertNoDuplicateLedgerIds(itemLedgerIds);

        OperatorInfo operator = operatorResolver.requireCurrentOperator();

        List<Long> ledgerIds = itemLedgerIds.stream().distinct().toList();
        Map<Long, MaterialLedger> lockedLedgers = materialStockMutationService.lockLedgersInOrder(ledgerIds);
        LocalDateTime operatedAt = resolveOperatedAt(dto.getOperatedAt(), lockedLedgers.values());

        List<MaterialIoRecordVO> created = new ArrayList<>();
        for (MaterialIoBatchItemDTO item : dto.getItems()) {
            MaterialIoSaveDTO saveDto = materialIoConverter.toSaveDto(item, ioType);
            MaterialIoRecord record = createRecord(saveDto, operator.getId(), operatedAt, lockedLedgers);
            MaterialLedger ledger = lockedLedgers.get(record.getMaterialLedgerId());
            created.add(materialIoConverter.toVo(record, ledger, operator));
        }
        return created;
    }

    @Override
    @Transactional
    public MaterialIoRecordVO update(Long id, MaterialIoUpdateDTO dto) {
        MaterialIoRecord existing = materialIoRecordMapper.selectByIdForUpdate(id);
        if (existing == null) {
            throw new MaterialIoNotFoundException(id);
        }

        String ioType = existing.getIoType();
        String purpose = dto.getPurpose();
        if (!org.springframework.util.StringUtils.hasText(purpose)) {
            purpose = existing.getPurpose();
        }
        MaterialIoPurpose.validatePurposeForIoType(ioType, purpose);
        purpose = MaterialIoPurpose.normalizePurpose(purpose);
        String projectRef = dto.getProjectRef();
        if (!org.springframework.util.StringUtils.hasText(projectRef)) {
            projectRef = existing.getProjectRef();
        }
        MaterialIoPurpose.validateProjectRef(purpose, projectRef);
        projectRef = MaterialIoPurpose.normalizeProjectRef(purpose, projectRef);

        Map<Long, MaterialLedger> lockedLedgers = materialStockMutationService.lockLedgersInOrder(List.of(existing.getMaterialLedgerId()));

        materialStockMutationService.reverseEffect(lockedLedgers.get(existing.getMaterialLedgerId()), existing);

        MaterialLedger targetLedger = lockedLedgers.get(existing.getMaterialLedgerId());
        if (targetLedger == null) {
            throw new BusinessException("物料台账不存在");
        }
        materialStockMutationService.validateOutbound(targetLedger, ioType, dto.getQuantity());
        materialStockMutationService.applyEffect(targetLedger, ioType, dto.getQuantity(), false);
        materialLedgerMapper.updateById(targetLedger);

        dto.setPurpose(purpose);
        dto.setProjectRef(projectRef);
        materialIoConverter.applyUpdateDto(existing, dto);
        materialIoRecordMapper.updateById(existing);

        OperatorInfo operator = operatorResolver.findById(existing.getOperatorUserId());
        return materialIoConverter.toVo(existing, targetLedger, operator);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MaterialIoRecord existing = materialIoRecordMapper.selectByIdForUpdate(id);
        if (existing == null) {
            throw new MaterialIoNotFoundException(id);
        }

        MaterialLedger ledger = materialLedgerMapper.selectByIdForUpdate(existing.getMaterialLedgerId());
        if (ledger != null) {
            materialStockMutationService.reverseEffect(ledger, existing);
            materialLedgerMapper.updateById(ledger);
        }
        materialIoRecordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDelete(BatchDeleteDTO dto) {
        if (CollectionUtils.isEmpty(dto.getIds())) {
            return;
        }
        List<Long> sortedIds = dto.getIds().stream().sorted().toList();
        for (Long id : sortedIds) {
            delete(id);
        }
    }

    private MaterialIoRecord createRecord(
            MaterialIoSaveDTO dto,
            Long operatorUserId,
            LocalDateTime operatedAt,
            Map<Long, MaterialLedger> lockedLedgers
    ) {
        String ioType = MaterialIoQueryBuilder.normalizeIoType(dto.getIoType());
        MaterialIoQueryBuilder.assertValidIoType(ioType);
        dto.setIoType(ioType);

        MaterialIoPurpose.validatePurposeForIoType(ioType, dto.getPurpose());
        String purpose = MaterialIoPurpose.normalizePurpose(dto.getPurpose());
        dto.setPurpose(purpose);
        MaterialIoPurpose.validateProjectRef(purpose, dto.getProjectRef());
        String projectRef = MaterialIoPurpose.normalizeProjectRef(purpose, dto.getProjectRef());
        dto.setProjectRef(projectRef);

        MaterialLedger ledger = lockedLedgers.get(dto.getMaterialLedgerId());
        if (ledger == null) {
            throw new BusinessException("物料台账不存在");
        }
        materialStockMutationService.validateOutbound(ledger, dto.getIoType(), dto.getQuantity());
        materialStockMutationService.applyEffect(ledger, dto.getIoType(), dto.getQuantity(), false);
        materialLedgerMapper.updateById(ledger);

        MaterialIoRecord entity = materialIoConverter.toNewEntity(
                dto.getMaterialLedgerId(),
                dto.getIoType(),
                dto.getQuantity(),
                dto.getRemark(),
                dto.getPurpose(),
                dto.getProjectRef(),
                operatorUserId,
                operatedAt
        );
        materialIoRecordMapper.insert(entity);
        return entity;
    }

    private LocalDateTime resolveOperatedAt(LocalDateTime operatedAt, MaterialLedger ledger) {
        return resolveOperatedAt(operatedAt, ledger == null ? List.of() : List.of(ledger));
    }

    private LocalDateTime resolveOperatedAt(LocalDateTime operatedAt, Collection<MaterialLedger> ledgers) {
        LocalDateTime resolved = operatedAt == null ? LocalDateTime.now() : operatedAt;
        LocalDateTime now = LocalDateTime.now();
        if (resolved.isAfter(now)) {
            throw new BusinessException("操作时间不能晚于当前时间");
        }
        for (MaterialLedger ledger : ledgers) {
            if (ledger != null && ledger.getCreatedAt() != null && resolved.isBefore(ledger.getCreatedAt())) {
                throw new BusinessException("操作时间不能早于物料台账创建时间");
            }
        }
        return resolved;
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

    private void assertNoDuplicateLedgerIds(List<Long> ledgerIds) {
        if (ledgerIds.size() != new HashSet<>(ledgerIds).size()) {
            throw new BusinessException("同一批次不能包含重复物料");
        }
    }
}
