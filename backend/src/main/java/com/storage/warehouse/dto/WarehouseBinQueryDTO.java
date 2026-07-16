package com.storage.warehouse.dto;

import lombok.Data;

import java.util.List;

@Data
public class WarehouseBinQueryDTO {

    private String binCode;

    private String rowNo;

    private Integer colNo;

    private Integer levelNo;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
