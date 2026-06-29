package com.storage.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SafetyStockQueryDTO extends MaterialQueryDTO {

    private String safetyQuantityKeyword;

    /** ALL / YES / NO */
    private String warningPeriod;
}
