package com.storage.dto;

import lombok.Data;

@Data
public class MaterialIoAggregateVO {

    private String ioType;

    private Long recordCount;

    private Long quantitySum;
}
