package com.storage.warehouse.io.dto;

import lombok.Data;

@Data
public class MaterialIoSafetyHintVO {

    private Long materialLedgerId;

    private Integer currentStock;

    private Integer safetyQuantity;

    private Boolean warningEnabled;
}
