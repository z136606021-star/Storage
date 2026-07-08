package com.storage.warehouse.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SafetyStockRecordVO {

    private Long safetyStockId;

    private Long materialLedgerId;

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private String model;

    private String binLocation;

    private Integer stockQuantity;

    private Integer safetyQuantity;

    private Boolean warningEnabled;

    private Boolean inWarningPeriod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
