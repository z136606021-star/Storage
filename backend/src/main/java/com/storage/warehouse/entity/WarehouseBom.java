package com.storage.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("warehouse_bom")
public class WarehouseBom {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String genericName;

    private String brand;

    private String name;

    private String remark;

    private String imageObjectKey;

    @TableField(exist = false)
    private String imageUrl;

    @TableField(exist = false)
    private List<String> imageObjectKeys;

    @TableField(exist = false)
    private List<String> imageUrls;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
