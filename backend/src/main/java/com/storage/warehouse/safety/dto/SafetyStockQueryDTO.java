package com.storage.warehouse.safety.dto;

import com.storage.warehouse.ledger.dto.MaterialQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SafetyStockQueryDTO extends MaterialQueryDTO {

    private String safetyQuantityKeyword;

    /** ALL / YES / NO */
    private String warningPeriod;
}
