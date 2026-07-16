package com.storage.warehouse.dto;

import lombok.Data;

import java.util.List;

@Data
public class WarehouseBomQueryDTO {

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
