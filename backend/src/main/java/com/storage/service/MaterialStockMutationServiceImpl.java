package com.storage.service;

import com.storage.dto.ImportResultVO;
import com.storage.dto.MaterialIoSaveDTO;
import com.storage.entity.MaterialIoRecord;
import com.storage.entity.MaterialLedger;
import com.storage.exception.BusinessException;
import com.storage.mapper.MaterialLedgerMapper;
import com.storage.query.MaterialIoQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MaterialStockMutationServiceImpl implements MaterialStockMutationService {

    private final MaterialLedgerMapper materialLedgerMapper;

    @Override
    public Map<Long, MaterialLedger> lockLedgersInOrder(List<Long> ledgerIds) {
        List<Long> sortedIds = ledgerIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        Map<Long, MaterialLedger> locked = new HashMap<>();
        for (Long ledgerId : sortedIds) {
            MaterialLedger ledger = materialLedgerMapper.selectByIdForUpdate(ledgerId);
            if (ledger == null) {
                throw new BusinessException("物料台账不存在: " + ledgerId);
            }
            locked.put(ledgerId, ledger);
        }
        return locked;
    }

    @Override
    public void reverseEffect(MaterialLedger ledger, MaterialIoRecord record) {
        if (ledger == null) {
            return;
        }
        applyEffect(ledger, record.getIoType(), record.getQuantity(), true);
    }

    @Override
    public void applyEffect(MaterialLedger ledger, String ioType, int quantity, boolean reverse) {
        int delta = MaterialIoQueryBuilder.isInbound(ioType) ? quantity : -quantity;
        if (reverse) {
            delta = -delta;
        }
        int current = ledger.getStockQuantity() == null ? 0 : ledger.getStockQuantity();
        int newStock = current + delta;
        if (newStock < 0) {
            throw new BusinessException(
                    "库存不足，当前库存: " + current + "，物料: " + ledger.getName() + " / " + ledger.getModel()
            );
        }
        ledger.setStockQuantity(newStock);
    }

    @Override
    public void validateOutbound(MaterialLedger ledger, String ioType, int quantity) {
        if (!MaterialIoQueryBuilder.isOutbound(ioType)) {
            return;
        }
        int current = ledger.getStockQuantity() == null ? 0 : ledger.getStockQuantity();
        if (quantity > current) {
            throw new BusinessException(
                    "出库数量不能超过当前库存，当前库存: " + current + "，物料: " + ledger.getName() + " / " + ledger.getModel()
            );
        }
    }

    @Override
    public List<ImportResultVO.ImportErrorVO> collectImportStockErrors(
            List<ImportStockSimulationRow> rows,
            Map<Long, MaterialLedger> ledgersById
    ) {
        Map<Long, Integer> simulatedStock = new HashMap<>();
        for (MaterialLedger ledger : ledgersById.values()) {
            simulatedStock.put(ledger.getId(), ledger.getStockQuantity() == null ? 0 : ledger.getStockQuantity());
        }

        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
        for (ImportStockSimulationRow row : rows) {
            MaterialIoSaveDTO dto = row.dto();
            MaterialLedger ledger = ledgersById.get(dto.getMaterialLedgerId());
            if (ledger == null) {
                continue;
            }
            int current = simulatedStock.getOrDefault(dto.getMaterialLedgerId(), 0);
            if (MaterialIoQueryBuilder.isOutbound(dto.getIoType())) {
                if (dto.getQuantity() > current) {
                    errors.add(new ImportResultVO.ImportErrorVO(
                            row.excelRow(),
                            "出库数量不能超过当前库存，当前库存: " + current + "，物料: "
                                    + ledger.getName() + " / " + ledger.getModel()
                    ));
                } else {
                    simulatedStock.put(dto.getMaterialLedgerId(), current - dto.getQuantity());
                }
            } else {
                simulatedStock.put(dto.getMaterialLedgerId(), current + dto.getQuantity());
            }
        }
        return errors;
    }
}
