package com.storage.dto;

import lombok.Data;

@Data
public class MaterialQueryDTO {

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private String model;

    private String binLocation;

    private Integer page = 1;

    private Integer pageSize = 10;
}
