package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.warehouse.dto.MaterialIoSaveDTO;
import com.storage.warehouse.entity.MaterialIoRecord;
import com.storage.warehouse.entity.MaterialLedger;

import java.util.List;
import java.util.Map;

public interface MaterialStockMutationService {

    record ImportStockSimulationRow(int excelRow, MaterialIoSaveDTO dto) {
    }

    Map<Long, MaterialLedger> lockLedgersInOrder(List<Long> ledgerIds);

    void reverseEffect(MaterialLedger ledger, MaterialIoRecord record);

    void applyEffect(MaterialLedger ledger, String ioType, int quantity, boolean reverse);

    void validateOutbound(MaterialLedger ledger, String ioType, int quantity);

    List<ImportResultVO.ImportErrorVO> collectImportStockErrors(
            List<ImportStockSimulationRow> rows,
            Map<Long, MaterialLedger> ledgersById
    );
}
