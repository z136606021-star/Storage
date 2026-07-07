package com.storage.warehouse.io.service;

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
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
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

@Service
@RequiredArgsConstructor
public class MaterialIoServiceImpl implements MaterialIoService {

    private final MaterialIoRecordMapper materialIoRecordMapper;
    private final MaterialLedgerMapper materialLedgerMapper;
    private final OperatorResolver operatorResolver;
    private final MaterialIoConverter materialIoConverter;
    private final MaterialIoReadService materialIoReadService;
    private final MaterialStockMutationService materialStockMutationService;

    @Override
    public PageResult<MaterialIoRecordVO> page(MaterialIoQueryDTO query) {
        return materialIoReadService.page(query);
    }

    @Override
    public MaterialIoRecordVO getById(Long id) {
        return materialIoReadService.getById(id);
    }

    @Override
    public List<MaterialIoRecordVO> listByQuery(MaterialIoQueryDTO query) {
        return materialIoReadService.listByQuery(query);
    }

    @Override
    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return materialIoReadService.filterOptions(query);
    }

    @Override
    public List<MaterialIoSafetyHintVO> safetyHints(List<Long> materialLedgerIds) {
        return materialIoReadService.safetyHints(materialLedgerIds);
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

    private void assertNoDuplicateLedgerIds(List<Long> ledgerIds) {
        if (ledgerIds.size() != new HashSet<>(ledgerIds).size()) {
            throw new BusinessException("同一批次不能包含重复物料");
        }
    }
}
