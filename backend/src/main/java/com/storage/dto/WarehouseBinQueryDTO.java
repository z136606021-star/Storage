package com.storage.dto;

import lombok.Data;

import java.util.List;

@Data
public class WarehouseBinQueryDTO {

    private String binCode;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
