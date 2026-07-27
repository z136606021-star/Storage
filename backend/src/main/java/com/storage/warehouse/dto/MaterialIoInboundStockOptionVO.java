package com.storage.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MaterialIoInboundStockOptionVO {

    private Long materialLedgerId;

    private String binLocation;

    private String model;

    private Integer stockQuantity;
}
