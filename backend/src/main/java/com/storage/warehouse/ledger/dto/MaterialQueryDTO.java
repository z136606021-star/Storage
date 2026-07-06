package com.storage.warehouse.ledger.dto;

import lombok.Data;

import java.util.List;

@Data
public class MaterialQueryDTO {

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private String model;

    private String binLocation;

    private List<Long> ids;

    private Integer page = 1;

    private Integer pageSize = 10;
}
