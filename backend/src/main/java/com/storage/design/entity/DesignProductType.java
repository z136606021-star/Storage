package com.storage.design.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("design_product_type")
public class DesignProductType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String typeCode;

    private String typeName;

    private Integer enabled;

    private Long operatorUserId;

    private String operatorName;

    private LocalDateTime operatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
