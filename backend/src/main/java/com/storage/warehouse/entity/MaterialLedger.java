package com.storage.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("material_ledger")
public class MaterialLedger {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String genericName;

    private String brand = "";

    private String name;

    private String model;

    private String binLocation;

    private Integer stockQuantity;

    private BigDecimal unitPrice;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
