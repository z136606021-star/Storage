package com.storage.warehouse.io.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import com.storage.warehouse.bin.entity.WarehouseBin;
import com.storage.warehouse.bin.mapper.WarehouseBinMapper;
import com.storage.warehouse.bom.entity.WarehouseBom;
import com.storage.warehouse.bom.mapper.WarehouseBomMapper;
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
import com.storage.warehouse.ledger.query.MaterialLedgerQueryBuilder;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private final WarehouseBomMapper warehouseBomMapper;
    private final WarehouseBinMapper warehouseBinMapper;
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
            LocalDateTime operatedAt = MaterialIoQueryBuilder.isInbound(dto.getIoType())
                    ? resolveOperatedAt(dto.getOperatedAt(), List.of())
                    : resolveOperatedAt(dto.getOperatedAt(), lockedLedgers.get(dto.getMaterialLedgerId()));
            createRecord(dto, operator.getId(), operatedAt, lockedLedgers);
        }
        return dtos.size();
    }

    @Override
    @Transactional
    public List<MaterialIoRecordVO> batchCreate(MaterialIoBatchSaveDTO dto) {
        String ioType = MaterialIoQueryBuilder.normalizeIoType(dto.getIoType());
        MaterialIoQueryBuilder.assertValidIoType(ioType);

        List<MaterialIoSaveDTO> saveDtos = dto.getItems().stream()
                .map(item -> toBatchSaveDto(item, ioType))
                .toList();

        List<Long> itemLedgerIds = saveDtos.stream()
                .map(MaterialIoSaveDTO::getMaterialLedgerId)
                .toList();
        assertNoDuplicateLedgerIds(itemLedgerIds);

        OperatorInfo operator = operatorResolver.requireCurrentOperator();

        List<Long> ledgerIds = itemLedgerIds.stream().distinct().toList();
        Map<Long, MaterialLedger> lockedLedgers = materialStockMutationService.lockLedgersInOrder(ledgerIds);
        LocalDateTime operatedAt = MaterialIoQueryBuilder.isInbound(ioType)
                ? resolveOperatedAt(dto.getOperatedAt(), List.of())
                : resolveOperatedAt(dto.getOperatedAt(), lockedLedgers.values());

        List<MaterialIoRecordVO> created = new ArrayList<>();
        for (MaterialIoSaveDTO saveDto : saveDtos) {
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

    private MaterialIoSaveDTO toBatchSaveDto(MaterialIoBatchItemDTO item, String ioType) {
        if (MaterialIoQueryBuilder.isInbound(ioType)) {
            Long materialLedgerId = resolveInboundLedgerId(item);
            MaterialIoSaveDTO dto = materialIoConverter.toSaveDto(item, ioType);
            dto.setMaterialLedgerId(materialLedgerId);
            return dto;
        }
        if (item.getMaterialLedgerId() == null) {
            throw new BusinessException("请选择物料台账");
        }
        return materialIoConverter.toSaveDto(item, ioType);
    }

    private Long resolveInboundLedgerId(MaterialIoBatchItemDTO item) {
        if (item.getBomId() == null) {
            throw new BusinessException("入库请选择物料清单");
        }
        if (!StringUtils.hasText(item.getBinLocation())) {
            throw new BusinessException("入库请选择Bin位");
        }

        WarehouseBom bom = warehouseBomMapper.selectById(item.getBomId());
        if (bom == null) {
            throw new BusinessException("物料清单不存在");
        }
        String binLocation = item.getBinLocation().trim();
        boolean binExists = warehouseBinMapper.selectCount(
                Wrappers.<WarehouseBin>lambdaQuery()
                        .eq(WarehouseBin::getBinCode, binLocation)
        ) > 0;
        if (!binExists) {
            throw new BusinessException("Bin位不存在: " + binLocation);
        }

        MaterialLedger existing = materialLedgerMapper.selectOne(MaterialLedgerQueryBuilder.byNaturalKey(
                bom.getCategory(),
                bom.getGenericName(),
                bom.getBrand(),
                bom.getName(),
                bom.getModel(),
                binLocation
        ));
        if (existing != null) {
            return existing.getId();
        }

        MaterialLedger ledger = new MaterialLedger();
        ledger.setCategory(bom.getCategory());
        ledger.setGenericName(bom.getGenericName());
        ledger.setBrand(StringUtils.hasText(bom.getBrand()) ? bom.getBrand().trim() : "");
        ledger.setName(bom.getName());
        ledger.setModel(bom.getModel());
        ledger.setBinLocation(binLocation);
        ledger.setStockQuantity(0);
        materialLedgerMapper.insert(ledger);
        return ledger.getId();
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
