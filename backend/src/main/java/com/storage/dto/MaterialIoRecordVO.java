package com.storage.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialIoRecordVO {

    private Long id;

    private Long materialLedgerId;

    private String ioType;

    private Integer quantity;

    private String remark;

    private String purpose;

    private String projectRef;

    private String purposeLabel;

    private Long operatorUserId;

    private String operatorUsername;

    private String operatorDisplayName;

    private LocalDateTime operatedAt;

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private String model;

    private String binLocation;

    private Integer stockQuantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
