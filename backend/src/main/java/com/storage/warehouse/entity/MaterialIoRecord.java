package com.storage.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_io_record")
public class MaterialIoRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialLedgerId;

    private String ioType;

    private Integer quantity;

    private String remark;

    private String purpose;

    private String projectRef;

    private Long operatorUserId;

    private LocalDateTime operatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
