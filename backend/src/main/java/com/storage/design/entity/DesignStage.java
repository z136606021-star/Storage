package com.storage.design.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("design_stage")
public class DesignStage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer sortOrder;

    private String stageName;

    private Integer enabled;

    private Long operatorUserId;

    private String operatorName;

    private LocalDateTime operatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
