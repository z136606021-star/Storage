package com.storage.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("safety_stock")
public class SafetyStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialLedgerId;

    private Integer safetyQuantity;

    private Boolean warningEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
