package com.storage.warehouse.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MaterialIoQueryDTO {

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private String model;

    private String binLocation;

    private String ioType;

    private String purpose;

    private String projectRef;

    private LocalDate operatedAtStart;

    private LocalDate operatedAtEnd;

    private List<Long> ids;

    private Long materialLedgerId;

    private Integer page = 1;

    private Integer pageSize = 10;
}
