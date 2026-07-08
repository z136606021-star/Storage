package com.storage.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("warehouse_bin")
public class WarehouseBin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String binCode;

    private Integer rowNo;

    private Integer colNo;

    private Integer levelNo;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
