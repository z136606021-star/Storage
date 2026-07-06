package com.storage.warehouse.io.dto;

import lombok.Data;

@Data
public class MaterialIoAggregateVO {

    private String ioType;

    private Long recordCount;

    private Long quantitySum;
}
